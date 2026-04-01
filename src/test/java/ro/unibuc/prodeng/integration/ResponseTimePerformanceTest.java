package ro.unibuc.prodeng.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ro.unibuc.prodeng.IntegrationTestBase;
import ro.unibuc.prodeng.repository.BudgetRepository;
import ro.unibuc.prodeng.repository.CategoryRepository;
import ro.unibuc.prodeng.repository.UserRepository;
import ro.unibuc.prodeng.repository.WalletRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de performanta pentru timpul de raspuns al endpoint-urilor API
 * Verifica daca operatiile CRUD, tranzactiile si cererile concurente se incadreaza in pragurile de timp acceptabile.
 */
class ResponseTimePerformanceTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Pragul maxim acceptabil pentru timpul mediu de raspuns (in milisecunde)
    private static final long RESPONSE_TIME_THRESHOLD_MS = 500;

    /**
     * Curata baza de date inainte de fiecare test pentru a asigura un mediu izolat si rezultate consistente
     * Ordinea stergerii respecta constrangerile de cheie externa
     */
    @BeforeEach
    void cleanDatabase() {
        budgetRepository.deleteAll();
        categoryRepository.deleteAll();
        walletRepository.deleteAll();
        userRepository.deleteAll();
    }

    /**
     * Testeaza performanta crearii de utilizatori prin 50 de iteratii
     * Masoara timpul fiecarei cereri POST si calculeaza statistici 
     * Verifica ca media se incadreaza sub pragul de 500ms
     */
    @Test
    void createUser_responseTimeUnderThreshold() throws Exception {
        // nr de cereri de creare utilizator 
        int iterations = 50;
        // se stocheaza timpii de raspuns pentru fiecare iteratie
        List<Long> times = new ArrayList<>();

        for (int i = 0; i < iterations; i++) {
            //  timpul de executie al fiecarei cereri POST
            long start = System.nanoTime();
            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"User" + i + "\",\"email\":\"user" + i + "@test.com\"}"))
                    .andExpect(status().isCreated());
            // conversie din nanosecunde in milisecunde
            long elapsed = (System.nanoTime() - start) / 1_000_000;
            times.add(elapsed);
        }

        // statistici agregate pe toate iteratiile
        LongSummaryStatistics stats = times.stream().mapToLong(Long::longValue).summaryStatistics();
        System.out.println("=== Create User Performance (" + iterations + " iterations) ===");
        System.out.println("  Avg: " + String.format("%.1f", stats.getAverage()) + " ms");
        System.out.println("  Min: " + stats.getMin() + " ms");
        System.out.println("  Max: " + stats.getMax() + " ms");

        // Verificam ca timpul mediu de raspuns nu depaseste pragul definit
        assertTrue(stats.getAverage() < RESPONSE_TIME_THRESHOLD_MS,
                "Average response time " + stats.getAverage() + " ms exceeds threshold " + RESPONSE_TIME_THRESHOLD_MS + " ms");
    }

    /**
     * Testeaza performanta operatiilor de depunere in bulk
     * Se creeaza un utilizator cu un portofel si un cont, apoi se executa 100 de depuneri succesive
     */
    @Test
    void createTransactions_bulkPerformance() throws Exception {
        String userId = createUser("BulkUser", "bulk@test.com");
        String walletId = createWallet(userId);
        String accountId = addAccount(walletId, "CHECKING", "RON", BigDecimal.valueOf(100000));

        // nr de tranzactii de depunere care vor fi executate
        int iterations = 100;
        List<Long> times = new ArrayList<>();

        for (int i = 0; i < iterations; i++) {
            // Fiecare depunere adauga 10 RON in cont
            String body = "{\"amount\":10.00}";
            long start = System.nanoTime();
            mockMvc.perform(patch("/api/wallets/{walletId}/accounts/{accountId}/deposit", walletId, accountId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());
            long elapsed = (System.nanoTime() - start) / 1_000_000;
            times.add(elapsed);
        }

        // statisticile de performanta pentru depunerile in bulk
        LongSummaryStatistics stats = times.stream().mapToLong(Long::longValue).summaryStatistics();
        System.out.println("=== Bulk Transactions Performance (" + iterations + " deposits) ===");
        System.out.println("  Avg: " + String.format("%.1f", stats.getAverage()) + " ms");
        System.out.println("  Min: " + stats.getMin() + " ms");
        System.out.println("  Max: " + stats.getMax() + " ms");
        System.out.println("  Total: " + stats.getSum() + " ms");

        // Pragul pentru tranzactii este mai strict decat cel general
        assertTrue(stats.getAverage() < 200,
                "Average transaction time " + stats.getAverage() + " ms exceeds 200 ms threshold");
    }

    /**
     * Testeaza performanta filtrarii tranzactiilor pe un set mare de date.
     */
    @Test
    void filterTransactions_performanceWithLargeDataset() throws Exception {
        // user cu portofel, cont si sold initial suficient
        String userId = createUser("FilterUser", "filter@test.com");
        String walletId = createWallet(userId);
        String accountId = addAccount(walletId, "CHECKING", "EUR", BigDecimal.valueOf(500000));

        // Inseram 200 de tranzactii pentru a simula un set mare de date
        for (int i = 0; i < 200; i++) {
            mockMvc.perform(patch("/api/wallets/{walletId}/accounts/{accountId}/deposit", walletId, accountId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"amount\":5.00}"))
                    .andExpect(status().isOk());
        }

        // facem 20 de interogari si masuram timpul fiecareia
        int queries = 20;
        List<Long> times = new ArrayList<>();

        for (int i = 0; i < queries; i++) {
            long start = System.nanoTime();
            // Filtram tranzactiile dupa userId pentru a testa performanta query-ului
            mockMvc.perform(get("/api/transactions")
                            .param("userId", userId))
                    .andExpect(status().isOk());
            long elapsed = (System.nanoTime() - start) / 1_000_000;
            times.add(elapsed);
        }

        // Afisam statisticile de performanta pentru interogari
        LongSummaryStatistics stats = times.stream().mapToLong(Long::longValue).summaryStatistics();
        System.out.println("=== Filter Transactions Performance (200 txns, " + queries + " queries) ===");
        System.out.println("  Avg: " + String.format("%.1f", stats.getAverage()) + " ms");
        System.out.println("  Min: " + stats.getMin() + " ms");
        System.out.println("  Max: " + stats.getMax() + " ms");

        // Verificam ca interogarea pe un set mare de date ramane sub pragul acceptabil
        assertTrue(stats.getAverage() < RESPONSE_TIME_THRESHOLD_MS,
                "Average query time " + stats.getAverage() + " ms exceeds threshold " + RESPONSE_TIME_THRESHOLD_MS + " ms");
    }

    /**
     * Test de throughput cu cereri concurente
     * Foloseste 10 thread-uri care executa simultan cate 10 cereri fiecare
     * Un CountDownLatch sincronizeaza pornirea simultana a tuturor thread-urilor
     * Verifica ca throughput-ul depaseste 5 req/sec si ca nu exista erori
     */
    @Test
    void concurrentRequests_throughputTest() throws Exception {
        // utilizator, portofel si cont cu sold mare
        String userId = createUser("ConcurrentUser", "concurrent@test.com");
        String walletId = createWallet(userId);
        String accountId = addAccount(walletId, "CHECKING", "RON", BigDecimal.valueOf(1000000));

        // testul de concurenta
        int threadCount = 10;          // Numarul de thread-uri concurente
        int requestsPerThread = 10;    // Numarul de cereri pe care le face fiecare thread
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        // Latch-ul sincronizeaza pornirea simultana a tuturor thread-urilor
        CountDownLatch startLatch = new CountDownLatch(1);
        // Contor atomic pentru a numara erorile din toate thread-urile
        AtomicInteger errorCount = new AtomicInteger(0);
        List<Future<List<Long>>> futures = new ArrayList<>();

        // create si submit task-urile pentru fiecare thread
        for (int t = 0; t < threadCount; t++) {
            futures.add(executor.submit(() -> {
                // Fiecare thread asteapta semnalul de start
                startLatch.await();
                List<Long> threadTimes = new ArrayList<>();
                for (int r = 0; r < requestsPerThread; r++) {
                    try {
                        // Masuram timpul fiecarei cereri de depunere
                        long start = System.nanoTime();
                        mockMvc.perform(patch("/api/wallets/{walletId}/accounts/{accountId}/deposit", walletId, accountId)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"amount\":1.00}"))
                                .andExpect(status().isOk());
                        long elapsed = (System.nanoTime() - start) / 1_000_000;
                        threadTimes.add(elapsed);
                    } catch (Exception e) {
                        // Numaram erorile fara a opri executia celorlalte cereri
                        errorCount.incrementAndGet();
                    }
                }
                return threadTimes;
            }));
        }

        // Pornim cronometrul si eliberam toate thread-urile simultan
        long totalStart = System.nanoTime();
        startLatch.countDown();

        // Colectam timpii de raspuns din toate thread-urile
        List<Long> allTimes = new ArrayList<>();
        for (Future<List<Long>> future : futures) {
            allTimes.addAll(future.get());
        }
        long totalElapsed = (System.nanoTime() - totalStart) / 1_000_000;
        executor.shutdown();

        // Calculam throughput-ul (cereri pe secunda) si statisticile
        int totalRequests = threadCount * requestsPerThread;
        double throughput = (double) totalRequests / (totalElapsed / 1000.0);
        LongSummaryStatistics stats = allTimes.stream().mapToLong(Long::longValue).summaryStatistics();

        // rezultatele testului de concurenta
        System.out.println("=== Concurrent Throughput Test ===");
        System.out.println("  Threads: " + threadCount + ", Requests/thread: " + requestsPerThread);
        System.out.println("  Total requests: " + totalRequests);
        System.out.println("  Total time: " + totalElapsed + " ms");
        System.out.println("  Throughput: " + String.format("%.1f", throughput) + " req/sec");
        System.out.println("  Avg response time: " + String.format("%.1f", stats.getAverage()) + " ms");
        System.out.println("  Errors: " + errorCount.get());

        // Verificam ca throughput-ul minim este de 5 cereri pe secunda
        assertTrue(throughput > 5, "Throughput " + throughput + " req/sec is below 5 req/sec minimum");
        // Verificam ca nu au aparut erori in timpul executiei concurente
        assertEquals(0, errorCount.get(), "There were " + errorCount.get() + " errors during concurrent execution");
    }

    /**
     * Testeaza timpul de raspuns pentru toate operatiile CRUD ale aplicatiei
     * Parcurge ciclul complet: creare utilizator -> portofel -> cont -> categorie -> buget -> tranzactie -> citire -> stergere, masurand fiecare operatie individual.
     * Afiseaza un tabel cu rezultatele si verifica ca fiecare operatie respecta pragul
     */
    @Test
    void endpointResponseTime_allCrudOperations() throws Exception {
        // Lista in care stocam perechile (endpoint, timp) pentru raportul final
        List<String[]> results = new ArrayList<>();

        // CREARE (POST) 

        // Creare utilizator nou
        long start = System.nanoTime();
        String userId = createUser("CrudUser", "crud@test.com");
        results.add(new String[]{"POST /api/users", String.valueOf((System.nanoTime() - start) / 1_000_000)});

        // Creare portofel asociat utilizatorului
        start = System.nanoTime();
        String walletId = createWallet(userId);
        results.add(new String[]{"POST /api/wallets/user/{userId}", String.valueOf((System.nanoTime() - start) / 1_000_000)});

        // Adaugare cont bancar in portofel (tip CHECKING, moneda RON, sold initial 5000)
        start = System.nanoTime();
        String accountId = addAccount(walletId, "CHECKING", "RON", BigDecimal.valueOf(5000));
        results.add(new String[]{"POST /api/wallets/{id}/accounts", String.valueOf((System.nanoTime() - start) / 1_000_000)});

        // Creare categorie de cheltuieli pentru utilizator
        start = System.nanoTime();
        String categoryId = createCategory(userId, "Food");
        results.add(new String[]{"POST /api/categories", String.valueOf((System.nanoTime() - start) / 1_000_000)});

        // Creare buget lunar asociat contului si categoriei (limita 1000, martie 2026)
        start = System.nanoTime();
        String budgetId = createBudget(walletId, accountId, categoryId, BigDecimal.valueOf(1000), 3, 2026);
        results.add(new String[]{"POST .../budgets", String.valueOf((System.nanoTime() - start) / 1_000_000)});

        // TRANZACTIE (PATCH) 

        // Depunere de 100 RON in cont
        start = System.nanoTime();
        mockMvc.perform(patch("/api/wallets/{walletId}/accounts/{accountId}/deposit", walletId, accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":100.00}"))
                .andExpect(status().isOk());
        results.add(new String[]{"PATCH .../deposit", String.valueOf((System.nanoTime() - start) / 1_000_000)});

        // CITIRE (GET)

        // detalii portofel
        start = System.nanoTime();
        mockMvc.perform(get("/api/wallets/{id}", walletId))
                .andExpect(status().isOk());
        results.add(new String[]{"GET /api/wallets/{id}", String.valueOf((System.nanoTime() - start) / 1_000_000)});

        // lista de tranzactii filtrate dupa utilizator
        start = System.nanoTime();
        mockMvc.perform(get("/api/transactions").param("userId", userId))
                .andExpect(status().isOk());
        results.add(new String[]{"GET /api/transactions", String.valueOf((System.nanoTime() - start) / 1_000_000)});

        // STERGERE (DELETE) 
        // Ordinea stergerii respecta dependentele: buget -> categorie -> portofel -> utilizator

        // Stergere buget
        start = System.nanoTime();
        mockMvc.perform(delete("/api/wallets/{walletId}/accounts/{accountId}/budgets/{budgetId}",
                        walletId, accountId, budgetId))
                .andExpect(status().isNoContent());
        results.add(new String[]{"DELETE .../budgets/{id}", String.valueOf((System.nanoTime() - start) / 1_000_000)});

        // Stergere categorie
        start = System.nanoTime();
        mockMvc.perform(delete("/api/categories/{id}", categoryId))
                .andExpect(status().isNoContent());
        results.add(new String[]{"DELETE /api/categories/{id}", String.valueOf((System.nanoTime() - start) / 1_000_000)});

        // Stergere portofel (si conturile asociate)
        start = System.nanoTime();
        mockMvc.perform(delete("/api/wallets/{id}", walletId))
                .andExpect(status().isNoContent());
        results.add(new String[]{"DELETE /api/wallets/{id}", String.valueOf((System.nanoTime() - start) / 1_000_000)});

        // Stergere utilizator
        start = System.nanoTime();
        mockMvc.perform(delete("/api/users/{id}", userId))
                .andExpect(status().isNoContent());
        results.add(new String[]{"DELETE /api/users/{id}", String.valueOf((System.nanoTime() - start) / 1_000_000)});

        // Afisam tabelul cu timpii de raspuns pentru fiecare endpoint
        System.out.println("=== Endpoint Response Time (All CRUD Operations) ===");
        System.out.printf("  %-35s %8s%n", "Endpoint", "Time (ms)");
        System.out.println("  " + "-".repeat(45));
        for (String[] row : results) {
            long time = Long.parseLong(row[1]);
            System.out.printf("  %-35s %8d%n", row[0], time);
            // Fiecare endpoint individual trebuie sa fie sub pragul de 500ms
            assertTrue(time < RESPONSE_TIME_THRESHOLD_MS,
                    row[0] + " took " + time + " ms, exceeding threshold of " + RESPONSE_TIME_THRESHOLD_MS + " ms");
        }
    }

    // Metode helper

    // Creeaza un utilizator nou si returneaza ID-ul 
    private String createUser(String name, String email) throws Exception {
        String body = objectMapper.writeValueAsString(
                new java.util.LinkedHashMap<>() {{
                    put("name", name);
                    put("email", email);
                }});

        MvcResult result = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    // Creeaza un portofel pentru un utilizator existent.

    private String createWallet(String userId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/wallets/user/{userId}", userId))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    // Adauga un cont bancar intr-un portofel existent.

    private String addAccount(String walletId, String type, String currency, BigDecimal initialBalance) throws Exception {
        String body = objectMapper.writeValueAsString(
                new java.util.LinkedHashMap<>() {{
                    put("type", type);
                    put("currency", currency);
                    put("initialBalance", initialBalance);
                }});

        MvcResult result = mockMvc.perform(post("/api/wallets/{id}/accounts", walletId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        // Luam ultimul cont din lista (cel tocmai adaugat)
        var accounts = objectMapper.readTree(result.getResponse().getContentAsString()).get("accounts");
        return accounts.get(accounts.size() - 1).get("id").asText();
    }

    // Creeaza o categorie de cheltuieli pentru un utilizator.

    private String createCategory(String userId, String name) throws Exception {
        String body = objectMapper.writeValueAsString(
                new java.util.LinkedHashMap<>() {{
                    put("userId", userId);
                    put("name", name);
                }});

        MvcResult result = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    // Creeaza un buget lunar pentru un cont si o categorie.
    private String createBudget(String walletId, String accountId, String categoryId,
                                BigDecimal amountLimit, int month, int year) throws Exception {
        String body = objectMapper.writeValueAsString(
                new java.util.LinkedHashMap<>() {{
                    put("categoryId", categoryId);
                    put("amountLimit", amountLimit);
                    put("month", month);
                    put("year", year);
                }});

        MvcResult result = mockMvc.perform(post("/api/wallets/{walletId}/accounts/{accountId}/budgets", walletId, accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
}
