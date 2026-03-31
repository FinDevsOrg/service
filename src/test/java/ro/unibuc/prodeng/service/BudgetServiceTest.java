package ro.unibuc.prodeng.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ro.unibuc.prodeng.exception.EntityNotFoundException;
import ro.unibuc.prodeng.model.AccountEntity;
import ro.unibuc.prodeng.model.Budget;
import ro.unibuc.prodeng.model.Category;
import ro.unibuc.prodeng.model.WalletEntity;
import ro.unibuc.prodeng.repository.BudgetRepository;
import ro.unibuc.prodeng.repository.CategoryRepository;
import ro.unibuc.prodeng.repository.WalletRepository;
import ro.unibuc.prodeng.request.SetBudgetRequest;
import ro.unibuc.prodeng.request.UpdateBudgetRequest;
import ro.unibuc.prodeng.response.BudgetResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private BudgetService budgetService;

    private WalletEntity walletWithAccount(String walletId, String userId, String accountId) {
        AccountEntity account = new AccountEntity(accountId, "CHECKING", "USD", BigDecimal.valueOf(1000), List.of());
        return new WalletEntity(walletId, userId, List.of(account));
    }

    private Budget budget(
            String id,
            String userId,
            String walletId,
            String accountId,
            String categoryId,
            BigDecimal limitAmount,
            int month,
            int year
    ) {
        return new Budget(id, userId, walletId, accountId, categoryId, limitAmount, month, year);
    }

    // Creates a new budget with a valid category - verifies all fields are saved correctly
    @Test
    void setBudget_withCategory_createsNewBudget() throws EntityNotFoundException {
        SetBudgetRequest request = new SetBudgetRequest("cat1", BigDecimal.valueOf(150), 3, 2026);
        WalletEntity wallet = walletWithAccount("w1", "u1", "a1");

        when(walletRepository.findById("w1")).thenReturn(Optional.of(wallet));
        when(categoryRepository.findByIdAndUserId("cat1", "u1")).thenReturn(Optional.of(new Category("cat1", "u1", "Food")));
        when(budgetRepository.findByWalletIdAndAccountIdAndCategoryIdAndMonthAndYear("w1", "a1", "cat1", 3, 2026))
                .thenReturn(Optional.empty());
        when(budgetRepository.save(any(Budget.class)))
                .thenReturn(budget("b1", "u1", "w1", "a1", "cat1", BigDecimal.valueOf(150), 3, 2026));

        BudgetResponse result = budgetService.setBudget("w1", "a1", request);

        assertEquals("b1", result.id());
        assertEquals("u1", result.userId());
        assertEquals("w1", result.walletId());
        assertEquals("a1", result.accountId());
        assertEquals("cat1", result.categoryId());
        assertEquals(BigDecimal.valueOf(150), result.limitAmount());
        assertEquals(3, result.month());
        assertEquals(2026, result.year());
    }

    // Blank category: finds existing uncategorized budget and updates its limit (no category lookup)
    @Test
    void setBudget_blankCategory_updatesExistingUncategorizedBudget() throws EntityNotFoundException {
        SetBudgetRequest request = new SetBudgetRequest("   ", BigDecimal.valueOf(200), 4, 2026);
        WalletEntity wallet = walletWithAccount("w1", "u1", "a1");
        Budget existing = budget("existing-id", "u1", "w1", "a1", null, BigDecimal.valueOf(100), 4, 2026);

        when(walletRepository.findById("w1")).thenReturn(Optional.of(wallet));
        when(budgetRepository.findByWalletIdAndAccountIdAndCategoryIdIsNullAndMonthAndYear("w1", "a1", 4, 2026))
                .thenReturn(Optional.of(existing));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BudgetResponse result = budgetService.setBudget("w1", "a1", request);

        assertEquals("existing-id", result.id());
        assertNull(result.categoryId());
        assertEquals(BigDecimal.valueOf(200), result.limitAmount());
        verify(categoryRepository, never()).findByIdAndUserId(any(), any());
    }

    // Category doesn't exist in DB - throws EntityNotFoundException, nothing is saved
    @Test
    void setBudget_missingCategory_throwsEntityNotFoundException() {
        SetBudgetRequest request = new SetBudgetRequest("cat-missing", BigDecimal.valueOf(150), 3, 2026);
        WalletEntity wallet = walletWithAccount("w1", "u1", "a1");

        when(walletRepository.findById("w1")).thenReturn(Optional.of(wallet));
        when(categoryRepository.findByIdAndUserId("cat-missing", "u1")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> budgetService.setBudget("w1", "a1", request));
        verify(budgetRepository, never()).save(any(Budget.class));
    }

    // Fetches an existing budget by ID - verifies correct fields are returned
    @Test
    void getBudget_existingBudget_returnsBudgetResponse() throws EntityNotFoundException {
        WalletEntity wallet = walletWithAccount("w1", "u1", "a1");
        Budget found = budget("b1", "u1", "w1", "a1", "cat1", BigDecimal.valueOf(120), 2, 2026);

        when(walletRepository.findById("w1")).thenReturn(Optional.of(wallet));
        when(budgetRepository.findByIdAndWalletIdAndAccountId("b1", "w1", "a1")).thenReturn(Optional.of(found));

        BudgetResponse result = budgetService.getBudget("w1", "a1", "b1");

        assertEquals("b1", result.id());
        assertEquals("cat1", result.categoryId());
        assertEquals(BigDecimal.valueOf(120), result.limitAmount());
    }

    // Existing wallet/account but missing budget ID - throws EntityNotFoundException
    @Test
    void getBudget_missingBudget_throwsEntityNotFoundException() {
        WalletEntity wallet = walletWithAccount("w1", "u1", "a1");
        when(walletRepository.findById("w1")).thenReturn(Optional.of(wallet));
        when(budgetRepository.findByIdAndWalletIdAndAccountId("b1", "w1", "a1")).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> budgetService.getBudget("w1", "a1", "b1")
        );

        assertTrue(exception.getMessage().contains("Budget: b1"));
    }

    // Updates only the limit amount - all other fields (category, month, year) stay the same
    @Test
    void updateBudget_existingBudget_updatesOnlyLimitAmount() throws EntityNotFoundException {
        WalletEntity wallet = walletWithAccount("w1", "u1", "a1");
        Budget existing = budget("b1", "u1", "w1", "a1", "cat1", BigDecimal.valueOf(100), 3, 2026);
        UpdateBudgetRequest request = new UpdateBudgetRequest(BigDecimal.valueOf(250));

        when(walletRepository.findById("w1")).thenReturn(Optional.of(wallet));
        when(budgetRepository.findByIdAndWalletIdAndAccountId("b1", "w1", "a1")).thenReturn(Optional.of(existing));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BudgetResponse result = budgetService.updateBudget("w1", "a1", "b1", request);

        assertEquals("b1", result.id());
        assertEquals("u1", result.userId());
        assertEquals("cat1", result.categoryId());
        assertEquals(3, result.month());
        assertEquals(2026, result.year());
        assertEquals(BigDecimal.valueOf(250), result.limitAmount());
    }

    // Existing wallet/account but missing budget ID - update fails and does not save
    @Test
    void updateBudget_missingBudget_throwsEntityNotFoundException() {
        WalletEntity wallet = walletWithAccount("w1", "u1", "a1");
        UpdateBudgetRequest request = new UpdateBudgetRequest(BigDecimal.valueOf(250));

        when(walletRepository.findById("w1")).thenReturn(Optional.of(wallet));
        when(budgetRepository.findByIdAndWalletIdAndAccountId("b1", "w1", "a1")).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> budgetService.updateBudget("w1", "a1", "b1", request)
        );

        assertTrue(exception.getMessage().contains("Budget: b1"));
        verify(budgetRepository, never()).save(any(Budget.class));
    }

    // No month/year filters - returns all budgets for the account
    @Test
    void getBudgets_withoutPeriod_returnsAllBudgetsForAccount() throws EntityNotFoundException {
        WalletEntity wallet = walletWithAccount("w1", "u1", "a1");
        Budget b1 = budget("b1", "u1", "w1", "a1", "cat1", BigDecimal.valueOf(100), 3, 2026);
        Budget b2 = budget("b2", "u1", "w1", "a1", null, BigDecimal.valueOf(200), 4, 2026);

        when(walletRepository.findById("w1")).thenReturn(Optional.of(wallet));
        when(budgetRepository.findByWalletIdAndAccountId("w1", "a1")).thenReturn(List.of(b1, b2));

        List<BudgetResponse> result = budgetService.getBudgets("w1", "a1", null, null);

        assertEquals(2, result.size());
        assertEquals("b1", result.get(0).id());
        assertEquals("b2", result.get(1).id());
    }

     // Filters budgets by month=3 and year=2026 - returns only matching budgets
    @Test
    void getBudgets_withPeriod_returnsFilteredBudgets() throws EntityNotFoundException {
        WalletEntity wallet = walletWithAccount("w1", "u1", "a1");
        Budget periodBudget = budget("b1", "u1", "w1", "a1", "cat1", BigDecimal.valueOf(100), 3, 2026);

        when(walletRepository.findById("w1")).thenReturn(Optional.of(wallet));
        when(budgetRepository.findByWalletIdAndAccountIdAndMonthAndYear("w1", "a1", 3, 2026))
                .thenReturn(List.of(periodBudget));

        List<BudgetResponse> result = budgetService.getBudgets("w1", "a1", 3, 2026);

        assertEquals(1, result.size());
        assertEquals("b1", result.get(0).id());
        assertEquals(3, result.get(0).month());
        assertEquals(2026, result.get(0).year());
    }

    // Providing month without year is invalid - throws IllegalArgumentException
    @Test
    void getBudgets_onlyMonthProvided_throwsIllegalArgumentException() throws EntityNotFoundException {
        WalletEntity wallet = walletWithAccount("w1", "u1", "a1");
        when(walletRepository.findById("w1")).thenReturn(Optional.of(wallet));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> budgetService.getBudgets("w1", "a1", 3, null)
        );

        assertTrue(exception.getMessage().contains("Both month and year are required"));
        verify(budgetRepository, never()).findByWalletIdAndAccountId(any(), any());
    }

    // Deletes an existing budget - verifies deleteById was called on the repository
    @Test
    void deleteBudget_existingBudget_deletesById() throws EntityNotFoundException {
        WalletEntity wallet = walletWithAccount("w1", "u1", "a1");
        Budget existing = budget("b1", "u1", "w1", "a1", "cat1", BigDecimal.valueOf(100), 3, 2026);

        when(walletRepository.findById("w1")).thenReturn(Optional.of(wallet));
        when(budgetRepository.findByIdAndWalletIdAndAccountId("b1", "w1", "a1")).thenReturn(Optional.of(existing));

        budgetService.deleteBudget("w1", "a1", "b1");

        verify(budgetRepository).deleteById("b1");
    }

    // Existing wallet/account but missing budget ID - delete fails and no delete call is made
    @Test
    void deleteBudget_missingBudget_throwsEntityNotFoundException() {
        WalletEntity wallet = walletWithAccount("w1", "u1", "a1");
        when(walletRepository.findById("w1")).thenReturn(Optional.of(wallet));
        when(budgetRepository.findByIdAndWalletIdAndAccountId("b1", "w1", "a1")).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> budgetService.deleteBudget("w1", "a1", "b1")
        );

        assertTrue(exception.getMessage().contains("Budget: b1"));
        verify(budgetRepository, never()).deleteById(any());
    }

    // Missing wallet - throws before any budget/category query
    @Test
    void setBudget_walletNotFound_throwsEntityNotFoundException() {
        SetBudgetRequest request = new SetBudgetRequest("cat1", BigDecimal.valueOf(150), 3, 2026);
        when(walletRepository.findById("w1")).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> budgetService.setBudget("w1", "a1", request)
        );

        assertTrue(exception.getMessage().contains("Wallet: w1"));
        verifyNoInteractions(categoryRepository);
        verify(budgetRepository, never()).save(any(Budget.class));
    }

    // Null category takes uncategorized path and skips category lookup
    @Test
    void setBudget_nullCategory_usesUncategorizedPath() throws EntityNotFoundException {
        SetBudgetRequest request = new SetBudgetRequest(null, BigDecimal.valueOf(200), 4, 2026);
        WalletEntity wallet = walletWithAccount("w1", "u1", "a1");

        when(walletRepository.findById("w1")).thenReturn(Optional.of(wallet));
        when(budgetRepository.findByWalletIdAndAccountIdAndCategoryIdIsNullAndMonthAndYear("w1", "a1", 4, 2026))
                .thenReturn(Optional.empty());
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BudgetResponse result = budgetService.setBudget("w1", "a1", request);

        assertNull(result.categoryId());
        assertEquals(BigDecimal.valueOf(200), result.limitAmount());
        verify(categoryRepository, never()).findByIdAndUserId(any(), any());
    }

    // Wallet exists but doesn't contain the requested account - throws EntityNotFoundException
    @Test
    void getBudget_missingAccountInWallet_throwsEntityNotFoundException() {
        WalletEntity wallet = walletWithAccount("w1", "u1", "different-account");
        when(walletRepository.findById("w1")).thenReturn(Optional.of(wallet));

        assertThrows(EntityNotFoundException.class, () -> budgetService.getBudget("w1", "a1", "b1"));
    }
}