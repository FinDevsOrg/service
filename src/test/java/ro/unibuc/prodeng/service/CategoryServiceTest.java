package ro.unibuc.prodeng.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ro.unibuc.prodeng.exception.EntityNotFoundException;
import ro.unibuc.prodeng.model.Category;
import ro.unibuc.prodeng.model.UserEntity;
import ro.unibuc.prodeng.repository.BudgetRepository;
import ro.unibuc.prodeng.repository.CategoryRepository;
import ro.unibuc.prodeng.request.CreateCategoryRequest;
import ro.unibuc.prodeng.request.UpdateCategoryRequest;
import ro.unibuc.prodeng.response.CategoryResponse;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void createCategory_validRequest_returnsCategoryResponse() throws EntityNotFoundException {
        CreateCategoryRequest request = new CreateCategoryRequest("user1", "Food");
        when(userService.getUserEntityById("user1")).thenReturn(new UserEntity("user1", "Alice", "alice@example.com"));
        when(categoryRepository.existsByUserIdAndNameIgnoreCase("user1", "Food")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(new Category("cat1", "user1", "Food"));

        CategoryResponse result = categoryService.createCategory(request);

        assertNotNull(result);
        assertEquals("cat1", result.id());
        assertEquals("user1", result.userId());
        assertEquals("Food", result.name());
    }

    @Test
    void createCategory_userNotFound_throwsEntityNotFoundException() throws EntityNotFoundException {
        CreateCategoryRequest request = new CreateCategoryRequest("unknown", "Food");
        when(userService.getUserEntityById("unknown")).thenThrow(new EntityNotFoundException("unknown"));

        assertThrows(EntityNotFoundException.class, () -> categoryService.createCategory(request));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void createCategory_duplicateName_throwsIllegalArgumentException() throws EntityNotFoundException {
        CreateCategoryRequest request = new CreateCategoryRequest("user1", "Food");
        when(userService.getUserEntityById("user1")).thenReturn(new UserEntity("user1", "Alice", "alice@example.com"));
        when(categoryRepository.existsByUserIdAndNameIgnoreCase("user1", "Food")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> categoryService.createCategory(request));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void getCategoriesByUserId_validUser_returnsList() throws EntityNotFoundException {
        when(userService.getUserEntityById("user1")).thenReturn(new UserEntity("user1", "Alice", "alice@example.com"));
        when(categoryRepository.findByUserId("user1")).thenReturn(List.of(
                new Category("cat1", "user1", "Food"),
                new Category("cat2", "user1", "Transport")
        ));

        List<CategoryResponse> result = categoryService.getCategoriesByUserId("user1");

        assertEquals(2, result.size());
        assertEquals("Food", result.get(0).name());
        assertEquals("Transport", result.get(1).name());
    }

    @Test
    void getCategoriesByUserId_userNotFound_throwsEntityNotFoundException() throws EntityNotFoundException {
        when(userService.getUserEntityById("unknown")).thenThrow(new EntityNotFoundException("unknown"));

        assertThrows(EntityNotFoundException.class, () -> categoryService.getCategoriesByUserId("unknown"));
    }

    @Test
    void updateCategory_validRequest_returnsUpdatedCategory() throws EntityNotFoundException {
        UpdateCategoryRequest request = new UpdateCategoryRequest("NewName");
        when(categoryRepository.findById("cat1")).thenReturn(Optional.of(new Category("cat1", "user1", "OldName")));
        when(categoryRepository.existsByUserIdAndNameIgnoreCase("user1", "NewName")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(new Category("cat1", "user1", "NewName"));

        CategoryResponse result = categoryService.updateCategory("cat1", request);

        assertEquals("NewName", result.name());
        assertEquals("cat1", result.id());
    }

    @Test
    void updateCategory_categoryNotFound_throwsEntityNotFoundException() {
        when(categoryRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> categoryService.updateCategory("unknown", new UpdateCategoryRequest("NewName")));
    }

    @Test
    void deleteCategory_validId_deletesCategoryAndBudgets() throws EntityNotFoundException {
        when(categoryRepository.findById("cat1")).thenReturn(Optional.of(new Category("cat1", "user1", "Food")));

        categoryService.deleteCategory("cat1");

        verify(budgetRepository, times(1)).deleteByCategoryId("cat1");
        verify(categoryRepository, times(1)).deleteById("cat1");
    }

    @Test
    void deleteCategory_categoryNotFound_throwsEntityNotFoundException() {
        when(categoryRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> categoryService.deleteCategory("unknown"));
        verify(categoryRepository, never()).deleteById(any());
    }
}
