package ro.unibuc.prodeng.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ro.unibuc.prodeng.exception.EntityNotFoundException;
import ro.unibuc.prodeng.model.Budget;
import ro.unibuc.prodeng.model.WalletEntity;
import ro.unibuc.prodeng.repository.BudgetRepository;
import ro.unibuc.prodeng.repository.CategoryRepository;
import ro.unibuc.prodeng.repository.WalletRepository;
import ro.unibuc.prodeng.request.SetBudgetRequest;
import ro.unibuc.prodeng.response.BudgetResponse;

@Service
public class BudgetService {

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    public BudgetResponse setBudget(String walletId, String accountId, SetBudgetRequest request) throws EntityNotFoundException {
        String safeWalletId = Objects.requireNonNull(walletId, "walletId must not be null");
        String safeAccountId = Objects.requireNonNull(accountId, "accountId must not be null");
        SetBudgetRequest safeRequest = Objects.requireNonNull(request, "request must not be null");

        WalletEntity wallet = getWalletWithAccount(safeWalletId, safeAccountId);
        String categoryId = normalizeCategoryId(safeRequest.categoryId());

        if (categoryId != null) {
            categoryRepository.findByIdAndUserId(categoryId, wallet.userId())
                    .orElseThrow(() -> new EntityNotFoundException("Category: " + categoryId));
        }

        Optional<Budget> existing = categoryId == null
                ? budgetRepository.findByWalletIdAndAccountIdAndCategoryIdIsNullAndMonthAndYear(
                        safeWalletId,
                        safeAccountId,
                        safeRequest.month(),
                        safeRequest.year()
                )
                : budgetRepository.findByWalletIdAndAccountIdAndCategoryIdAndMonthAndYear(
                        safeWalletId,
                        safeAccountId,
                        categoryId,
                        safeRequest.month(),
                        safeRequest.year()
                );

        Budget budgetToSave = new Budget(
                existing.map(Budget::id).orElse(null),
                wallet.userId(),
                safeWalletId,
                safeAccountId,
                categoryId,
                safeRequest.amountLimit(),
                safeRequest.month(),
                safeRequest.year()
        );

        Budget saved = budgetRepository.save(budgetToSave);
        return toResponse(saved);
    }

    public List<BudgetResponse> getBudgets(String walletId, String accountId, Integer month, Integer year)
            throws EntityNotFoundException {
        String safeWalletId = Objects.requireNonNull(walletId, "walletId must not be null");
        String safeAccountId = Objects.requireNonNull(accountId, "accountId must not be null");

        getWalletWithAccount(safeWalletId, safeAccountId);

        if ((month == null) != (year == null)) {
            throw new IllegalArgumentException("Both month and year are required when filtering by period");
        }

        List<Budget> budgets = month == null
                ? budgetRepository.findByWalletIdAndAccountId(safeWalletId, safeAccountId)
                : budgetRepository.findByWalletIdAndAccountIdAndMonthAndYear(
                        safeWalletId,
                        safeAccountId,
                Objects.requireNonNull(month, "month must not be null"),
                Objects.requireNonNull(year, "year must not be null")
                );

        return budgets.stream().map(this::toResponse).toList();
    }

    public void deleteBudget(String walletId, String accountId, String budgetId) throws EntityNotFoundException {
        String safeWalletId = Objects.requireNonNull(walletId, "walletId must not be null");
        String safeAccountId = Objects.requireNonNull(accountId, "accountId must not be null");
        String safeBudgetId = Objects.requireNonNull(budgetId, "budgetId must not be null");

        getWalletWithAccount(safeWalletId, safeAccountId);

        Budget budget = budgetRepository.findByIdAndWalletIdAndAccountId(safeBudgetId, safeWalletId, safeAccountId)
                .orElseThrow(() -> new EntityNotFoundException("Budget: " + safeBudgetId));

        String persistedBudgetId = Objects.requireNonNull(budget.id(), "budget id must not be null");
        budgetRepository.deleteById(persistedBudgetId);
    }

    private WalletEntity getWalletWithAccount(String walletId, String accountId) throws EntityNotFoundException {
        String safeWalletId = Objects.requireNonNull(walletId, "walletId must not be null");

        WalletEntity wallet = walletRepository.findById(safeWalletId)
            .orElseThrow(() -> new EntityNotFoundException("Wallet: " + safeWalletId));

        boolean accountExists = wallet.accounts().stream().anyMatch(account -> accountId.equals(account.id()));
        if (!accountExists) {
            throw new EntityNotFoundException("Account: " + accountId);
        }

        return wallet;
    }

    private String normalizeCategoryId(String categoryId) {
        if (categoryId == null) {
            return null;
        }
        String trimmed = categoryId.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private BudgetResponse toResponse(Budget budget) {
        return new BudgetResponse(
                budget.id(),
                budget.userId(),
                budget.walletId(),
                budget.accountId(),
                budget.categoryId(),
                budget.limitAmount(),
                budget.month(),
                budget.year()
        );
    }
}
