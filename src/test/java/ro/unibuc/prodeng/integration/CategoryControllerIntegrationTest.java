package ro.unibuc.prodeng.integration;

import com.fasterxml.jackson.databind.ObjectMapper;

import ro.unibuc.prodeng.IntegrationTestBase;
import ro.unibuc.prodeng.repository.CategoryRepository;
import ro.unibuc.prodeng.repository.UserRepository;
import ro.unibuc.prodeng.request.CreateCategoryRequest;
import ro.unibuc.prodeng.request.CreateUserRequest;
import ro.unibuc.prodeng.request.UpdateCategoryRequest;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("CategoryController Integration Tests")
class CategoryControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanUp() {
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String createUser(String name, String email) throws Exception {
        //construieste obiectul de request
        CreateUserRequest request = new CreateUserRequest(name, email);
        //trimite POST /api/users cu requestul ca json in body
        String response = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated()) //verifica ca statusul e 201 Created
                .andReturn().getResponse().getContentAsString(); // ia raspunsul ca string

        return objectMapper.readTree(response).get("id").asText();  // returneaza id-ul userului creat
    }

    private String createCategory(String userId, String name) throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest(userId, name);

        String response = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId))  // verifica userId in raspuns
                .andExpect(jsonPath("$.name").value(name)) // verifica numele categoriei in raspuns
                .andExpect(jsonPath("$.id").exists()) // verifica ca exista id-ul categoriei
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asText(); // returneaza id-ul categoriei
    }

    @Test
    void testCreateAndGetCategory_validCategory_retrievesSuccessfully() throws Exception {
        // Arrange
        String userId = createUser("Alice", "alice@example.com");
        createCategory(userId, "Food");

        // Act & Assert
        // trimite GET /api/categories?userId=abc123
        mockMvc.perform(get("/api/categories").param("userId", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Food"))
                .andExpect(jsonPath("$[0].userId").value(userId));
    }

    // verifica ca daca un user are mai multe categorii, toate sunt returnate corect
    @Test
    void testGetCategories_multipleCategories_returnsAll() throws Exception {
        // Arrange
        String userId = createUser("Alice", "alice@example.com");
        createCategory(userId, "Food");
        createCategory(userId, "Transport");

        // Act & Assert
        mockMvc.perform(get("/api/categories").param("userId", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // verifica ca dupa ce faci update la o categorie, numele se schimba in baza de date
    @Test
    void testUpdateCategory_validRequest_updatesName() throws Exception {
        // Arrange
        String userId = createUser("Alice", "alice@example.com");
        String categoryId = createCategory(userId, "Food");

        // Act & Assert
        mockMvc.perform(patch("/api/categories/" + categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateCategoryRequest("Groceries"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Groceries"));
    }

    // verifica ca dupa stergere, categoria dispare din db
    @Test
    void testDeleteCategory_existingCategory_deletesSuccessfully() throws Exception {
        // Arrange
        String userId = createUser("Alice", "alice@example.com");
        String categoryId = createCategory(userId, "Food");

        // Act & Assert
        mockMvc.perform(delete("/api/categories/" + categoryId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/categories").param("userId", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // verifica ca nu poti crea o categorie pentru un user inexistent
    @Test
    void testCreateCategory_nonExistentUser_returnsNotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCategoryRequest("nonexistent-id", "Food"))))
                .andExpect(status().isNotFound());
    }

    // verifica ca nu poti crea doua categorii cu acelasi nume pentru acelasi user
    @Test
    void testCreateCategory_duplicateName_returnsBadRequest() throws Exception {
        // Arrange
        String userId = createUser("Alice", "alice@example.com");
        createCategory(userId, "Food");

        // Act & Assert
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCategoryRequest(userId, "Food"))))
                .andExpect(status().isBadRequest());
    }

    // verifica ca nu poti face update la o categorie care nu exista
    @Test
    void testUpdateCategory_nonExistentCategory_returnsNotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(patch("/api/categories/nonexistent-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateCategoryRequest("NewName"))))
                .andExpect(status().isNotFound());
    }

    // verifica ca nu poti sterge o categorie care nu exista
    @Test
    void testDeleteCategory_nonExistentCategory_returnsNotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/categories/nonexistent-id"))
                .andExpect(status().isNotFound());
    }
}
