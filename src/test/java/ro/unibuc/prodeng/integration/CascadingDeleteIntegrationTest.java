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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CascadingDeleteIntegrationTest extends IntegrationTestBase {

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

    @BeforeEach
    void cleanDatabase() {
        budgetRepository.deleteAll();
        categoryRepository.deleteAll();
        walletRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void deleteUser_deletesAllAssociatedEntities() throws Exception {
        // Create user
        String userId = createUser("Alice", "alice@test.com");

        // Create wallet
        String walletId = createWallet(userId);

        // Add account
        String accountId = addAccount(walletId, "CHECKING", "RON", BigDecimal.valueOf(1000));

        // Create category
        String categoryId = createCategory(userId, "Food");

        // Create budget (category-level)
        createBudget(walletId, accountId, categoryId, BigDecimal.valueOf(500), 3, 2026);

        // Create budget (account-level)
        createBudget(walletId, accountId, null, BigDecimal.valueOf(800), 3, 2026);

        // Verify everything exists before delete
        assertTrue(userRepository.existsById(userId));
        assertTrue(walletRepository.existsByUserId(userId));
        assertEquals(1, categoryRepository.findByUserId(userId).size());
        assertEquals(2, budgetRepository.findByWalletIdAndAccountId(walletId, accountId).size());

        // Act: delete user
        mockMvc.perform(delete("/api/users/{id}", userId))
                .andExpect(status().isNoContent());

        // Assert: user is gone
        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isNotFound());

        // Assert: wallet is gone
        mockMvc.perform(get("/api/wallets/user/{userId}", userId))
                .andExpect(status().isNotFound());

        // Assert: categories are gone
        assertEquals(0, categoryRepository.findByUserId(userId).size());

        // Assert: budgets are gone
        assertEquals(0, budgetRepository.findByWalletIdAndAccountId(walletId, accountId).size());

        // Assert: nothing remains in repositories for this user
        assertTrue(userRepository.findById(userId).isEmpty());
        assertTrue(walletRepository.findByUserId(userId).isEmpty());
    }

    @Test
    void deleteCategory_deletesAssociatedBudgets_keepsAccountBudget() throws Exception {
        // Setup
        String userId = createUser("Bob", "bob@test.com");
        String walletId = createWallet(userId);
        String accountId = addAccount(walletId, "CHECKING", "EUR", BigDecimal.valueOf(2000));
        String categoryId = createCategory(userId, "Transport");

        // Create category-level budget
        createBudget(walletId, accountId, categoryId, BigDecimal.valueOf(300), 3, 2026);
        // Create account-level budget (no category)
        createBudget(walletId, accountId, null, BigDecimal.valueOf(1000), 3, 2026);

        assertEquals(2, budgetRepository.findByWalletIdAndAccountId(walletId, accountId).size());

        // Act: delete category
        mockMvc.perform(delete("/api/categories/{id}", categoryId))
                .andExpect(status().isNoContent());

        // Assert: category is gone
        assertTrue(categoryRepository.findById(categoryId).isEmpty());

        // Assert: category budget is gone, account budget remains
        var remainingBudgets = budgetRepository.findByWalletIdAndAccountId(walletId, accountId);
        assertEquals(1, remainingBudgets.size());
        assertTrue(remainingBudgets.getFirst().categoryId() == null);

        // Assert: wallet and account are untouched
        mockMvc.perform(get("/api/wallets/{id}", walletId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accounts.length()").value(1));
    }

    @Test
    void deleteWallet_deletesAssociatedBudgets_keepsUserAndCategories() throws Exception {
        // Setup
        String userId = createUser("Carol", "carol@test.com");
        String walletId = createWallet(userId);
        String accountId = addAccount(walletId, "SAVINGS", "USD", BigDecimal.valueOf(5000));
        String categoryId = createCategory(userId, "Rent");

        createBudget(walletId, accountId, categoryId, BigDecimal.valueOf(1500), 3, 2026);
        createBudget(walletId, accountId, null, BigDecimal.valueOf(3000), 3, 2026);

        assertEquals(2, budgetRepository.findByWalletIdAndAccountId(walletId, accountId).size());

        // Act: delete wallet
        mockMvc.perform(delete("/api/wallets/{id}", walletId))
                .andExpect(status().isNoContent());

        // Assert: wallet is gone
        mockMvc.perform(get("/api/wallets/{id}", walletId))
                .andExpect(status().isNotFound());

        // Assert: all budgets for this wallet are gone
        assertEquals(0, budgetRepository.findByWalletIdAndAccountId(walletId, accountId).size());

        // Assert: user still exists
        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk());

        // Assert: category still exists
        assertEquals(1, categoryRepository.findByUserId(userId).size());
    }

    @Test
    void deleteAccount_deletesOnlyThatAccountBudgets_keepsOtherAccount() throws Exception {
        // Setup
        String userId = createUser("Dan", "dan@test.com");
        String walletId = createWallet(userId);
        String accountId1 = addAccount(walletId, "CHECKING", "RON", BigDecimal.valueOf(1000));
        String accountId2 = addAccount(walletId, "SAVINGS", "RON", BigDecimal.valueOf(2000));

        // Budgets for account 1
        createBudget(walletId, accountId1, null, BigDecimal.valueOf(500), 3, 2026);
        // Budgets for account 2
        createBudget(walletId, accountId2, null, BigDecimal.valueOf(800), 3, 2026);

        assertEquals(1, budgetRepository.findByWalletIdAndAccountId(walletId, accountId1).size());
        assertEquals(1, budgetRepository.findByWalletIdAndAccountId(walletId, accountId2).size());

        // Act: delete account 1
        mockMvc.perform(delete("/api/wallets/{walletId}/accounts/{accountId}", walletId, accountId1))
                .andExpect(status().isNoContent());

        // Assert: account 1 budgets are gone
        assertEquals(0, budgetRepository.findByWalletIdAndAccountId(walletId, accountId1).size());

        // Assert: account 2 budgets remain
        assertEquals(1, budgetRepository.findByWalletIdAndAccountId(walletId, accountId2).size());

        // Assert: wallet still exists with only account 2
        mockMvc.perform(get("/api/wallets/{id}", walletId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accounts.length()").value(1))
                .andExpect(jsonPath("$.accounts[0].id").value(accountId2));
    }

    @Test
    void deleteUser_withNoAssociatedData_succeeds() throws Exception {
        // Setup: just a user, no wallet/categories/budgets
        String userId = createUser("Eve", "eve@test.com");

        assertTrue(userRepository.existsById(userId));

        // Act
        mockMvc.perform(delete("/api/users/{id}", userId))
                .andExpect(status().isNoContent());

        // Assert
        assertTrue(userRepository.findById(userId).isEmpty());
        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isNotFound());
    }

    //  Helper methods 

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

    private String createWallet(String userId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/wallets/user/{userId}", userId))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

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

        var accounts = objectMapper.readTree(result.getResponse().getContentAsString()).get("accounts");
        return accounts.get(accounts.size() - 1).get("id").asText();
    }

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
