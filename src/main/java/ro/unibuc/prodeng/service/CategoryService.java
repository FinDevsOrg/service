package ro.unibuc.prodeng.service;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ro.unibuc.prodeng.exception.EntityNotFoundException;
import ro.unibuc.prodeng.model.Category;
import ro.unibuc.prodeng.repository.BudgetRepository;
import ro.unibuc.prodeng.repository.CategoryRepository;
import ro.unibuc.prodeng.request.CreateCategoryRequest;
import ro.unibuc.prodeng.response.CategoryResponse;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private UserService userService;

    public CategoryResponse createCategory(CreateCategoryRequest request) throws EntityNotFoundException {
        CreateCategoryRequest safeRequest = Objects.requireNonNull(request, "request must not be null");

        String safeUserId = Objects.requireNonNull(safeRequest.userId(), "userId must not be null").trim();
        String safeName = Objects.requireNonNull(safeRequest.name(), "name must not be null").trim();

        userService.getUserEntityById(safeUserId);

        if (categoryRepository.existsByUserIdAndNameIgnoreCase(safeUserId, safeName)) {
            throw new IllegalArgumentException("Category already exists for user: " + safeName);
        }

        Category saved = categoryRepository.save(new Category(null, safeUserId, safeName));
        return toResponse(saved);
    }

    public List<CategoryResponse> getCategoriesByUserId(String userId) throws EntityNotFoundException {
        String safeUserId = Objects.requireNonNull(userId, "userId must not be null");

        userService.getUserEntityById(safeUserId);

        return categoryRepository.findByUserId(safeUserId).stream()
                .map(this::toResponse)
                .toList();
    }

    public void deleteCategory(String id) throws EntityNotFoundException {
        String safeId = Objects.requireNonNull(id, "id must not be null");

        Category category = categoryRepository.findById(safeId)
                .orElseThrow(() -> new EntityNotFoundException("Category: " + safeId));

        String persistedCategoryId = Objects.requireNonNull(category.id(), "category id must not be null");

        budgetRepository.deleteByCategoryId(persistedCategoryId);
        categoryRepository.deleteById(persistedCategoryId);
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.id(),
                category.userId(),
                category.name()
        );
    }
}
