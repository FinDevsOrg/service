package ro.unibuc.prodeng.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ro.unibuc.prodeng.exception.EntityNotFoundException;
import ro.unibuc.prodeng.exception.GlobalExceptionHandler;
import ro.unibuc.prodeng.request.CreateCategoryRequest;
import ro.unibuc.prodeng.request.UpdateCategoryRequest;
import ro.unibuc.prodeng.response.CategoryResponse;
import ro.unibuc.prodeng.service.CategoryService;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
class CategoryControllerTest {

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController categoryController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    private CategoryResponse cat1 = new CategoryResponse("cat1", "user1", "Food");
    private CategoryResponse cat2 = new CategoryResponse("cat2", "user1", "Transport");
    private CreateCategoryRequest createRequest = new CreateCategoryRequest("user1", "Food");
    private UpdateCategoryRequest updateRequest = new UpdateCategoryRequest("NewName");

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(categoryController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createCategory_validRequest_returnsCreated() throws Exception {
        when(categoryService.createCategory(any(CreateCategoryRequest.class))).thenReturn(cat1);

        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is("cat1")))
                .andExpect(jsonPath("$.userId", is("user1")))
                .andExpect(jsonPath("$.name", is("Food")));

        verify(categoryService, times(1)).createCategory(any(CreateCategoryRequest.class));
    }

    @Test
    void createCategory_userNotFound_returnsNotFound() throws Exception {
        when(categoryService.createCategory(any(CreateCategoryRequest.class)))
                .thenThrow(new EntityNotFoundException("user1"));

        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCategoriesByUserId_validUserId_returnsOk() throws Exception {
        when(categoryService.getCategoriesByUserId("user1")).thenReturn(List.of(cat1, cat2));

        mockMvc.perform(get("/api/categories")
                .param("userId", "user1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is("cat1")))
                .andExpect(jsonPath("$[1].id", is("cat2")));

        verify(categoryService, times(1)).getCategoriesByUserId("user1");
    }

    @Test
    void getCategoriesByUserId_userNotFound_returnsNotFound() throws Exception {
        when(categoryService.getCategoriesByUserId("unknown"))
                .thenThrow(new EntityNotFoundException("unknown"));

        mockMvc.perform(get("/api/categories")
                .param("userId", "unknown")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateCategory_validRequest_returnsOk() throws Exception {
        CategoryResponse updated = new CategoryResponse("cat1", "user1", "NewName");
        when(categoryService.updateCategory(eq("cat1"), any(UpdateCategoryRequest.class))).thenReturn(updated);

        mockMvc.perform(patch("/api/categories/cat1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("NewName")));

        verify(categoryService, times(1)).updateCategory(eq("cat1"), any(UpdateCategoryRequest.class));
    }

    @Test
    void updateCategory_categoryNotFound_returnsNotFound() throws Exception {
        when(categoryService.updateCategory(eq("unknown"), any(UpdateCategoryRequest.class)))
                .thenThrow(new EntityNotFoundException("unknown"));

        mockMvc.perform(patch("/api/categories/unknown")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteCategory_validId_returnsNoContent() throws Exception {
        doNothing().when(categoryService).deleteCategory("cat1");

        mockMvc.perform(delete("/api/categories/cat1"))
                .andExpect(status().isNoContent());

        verify(categoryService, times(1)).deleteCategory("cat1");
    }

    @Test
    void deleteCategory_categoryNotFound_returnsNotFound() throws Exception {
        doThrow(new EntityNotFoundException("unknown")).when(categoryService).deleteCategory("unknown");

        mockMvc.perform(delete("/api/categories/unknown"))
                .andExpect(status().isNotFound());
    }
}
