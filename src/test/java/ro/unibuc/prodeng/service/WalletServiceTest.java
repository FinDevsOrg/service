package ro.unibuc.prodeng.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ro.unibuc.prodeng.exception.EntityNotFoundException;
import ro.unibuc.prodeng.model.AccountEntity;
import ro.unibuc.prodeng.model.Budget;
import ro.unibuc.prodeng.model.Category;
import ro.unibuc.prodeng.model.Transaction;
import ro.unibuc.prodeng.model.TransactionType;
import ro.unibuc.prodeng.model.UserEntity;
import ro.unibuc.prodeng.model.WalletEntity;
import ro.unibuc.prodeng.repository.BudgetRepository;
import ro.unibuc.prodeng.repository.CategoryRepository;
import ro.unibuc.prodeng.repository.WalletRepository;
import ro.unibuc.prodeng.request.CreateAccountRequest;
import ro.unibuc.prodeng.request.TransactionRequest;
import ro.unibuc.prodeng.request.UpdateAccountBalanceRequest;
import ro.unibuc.prodeng.response.AccountResponse;
import ro.unibuc.prodeng.response.TransactionResponse;
import ro.unibuc.prodeng.response.WalletResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private WalletService walletService;

    private WalletEntity wallet(String walletId, String userId, List<AccountEntity> accounts) {
        return new WalletEntity(walletId, userId, accounts);
    }

    private AccountEntity account(String id, BigDecimal balance, List<Transaction> transactions) {
        return new AccountEntity(id, "CHECKING", "RON", balance, transactions);
    }

    private Transaction transaction(
            String id,
            String walletId,
            String accountId,
            String categoryId,
            BigDecimal amount,
            TransactionType type,
            LocalDateTime date
    ) {
        return new Transaction(id, walletId, accountId, categoryId, amount, type, date, "desc", "user1");
    }

    // Valid user without a wallet - creates new empty wallet and returns it
    @Test
    void createWallet_validUser_createsNewWallet() {
        when(userService.getUserEntityById("user1")).thenReturn(new UserEntity("user1", "Alice", "alice@mail.com"));
        when(walletRepository.existsByUserId("user1")).thenReturn(false);
        when(walletRepository.save(any(WalletEntity.class))).thenAnswer(invocation -> {
            WalletEntity saved = invocation.getArgument(0);
            return new WalletEntity("w1", saved.userId(), saved.accounts());
        });

        WalletResponse result = walletService.createWallet("user1");

        assertEquals("w1", result.id());
        assertEquals("user1", result.userId());
        assertEquals(0, result.accounts().size());
    }

    // User already has a wallet - throws IllegalArgumentException, nothing saved
    @Test
    void createWallet_existingWalletForUser_throwsIllegalArgumentException() {
        when(userService.getUserEntityById("user1")).thenReturn(new UserEntity("user1", "Alice", "alice@mail.com"));
        when(walletRepository.existsByUserId("user1")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.createWallet("user1")
        );

        assertTrue(exception.getMessage().contains("Wallet already exists"));
        verify(walletRepository, never()).save(any(WalletEntity.class));
    }


    // Fetch wallet by ID - returns wallet with its account
    @Test
    void getWalletById_existingWallet_returnsWalletResponse() {
        WalletEntity existing = wallet("w1", "user1", List.of(account("a1", BigDecimal.valueOf(100), List.of())));
        when(walletRepository.findById("w1")).thenReturn(Optional.of(existing));

        WalletResponse result = walletService.getWalletById("w1");

        assertEquals("w1", result.id());
        assertEquals("user1", result.userId());
        assertEquals(1, result.accounts().size());
    }

    // Wallet ID not found - throws EntityNotFoundException
    @Test
    void getWalletById_missingWallet_throwsEntityNotFoundException() {
        when(walletRepository.findById("missing")).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> walletService.getWalletById("missing")
        );

        assertTrue(exception.getMessage().contains("Wallet: missing"));
    }

    // Fetch wallet by user ID - returns the user's wallet
    @Test
    void getWalletByUserId_existingWallet_returnsWalletResponse() {
        WalletEntity existing = wallet("w1", "user1", List.of());
        when(walletRepository.findByUserId("user1")).thenReturn(Optional.of(existing));

        WalletResponse result = walletService.getWalletByUserId("user1");

        assertEquals("w1", result.id());
        assertEquals("user1", result.userId());
    }

    // No wallet for this user - throws EntityNotFoundException
    @Test
    void getWalletByUserId_missingWallet_throwsEntityNotFoundException() {
        when(walletRepository.findByUserId("missing-user")).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> walletService.getWalletByUserId("missing-user")
        );

        assertTrue(exception.getMessage().contains("Wallet for user: missing-user"));
    }

    // Adds a new CHECKING/RON account with balance 200 to an empty wallet
    @Test
    void addAccount_validRequest_addsAccountAndSavesWallet() {
        WalletEntity existing = wallet("w1", "user1", List.of());
        CreateAccountRequest request = new CreateAccountRequest("CHECKING", "RON", BigDecimal.valueOf(200));

        when(walletRepository.findById("w1")).thenReturn(Optional.of(existing));
        when(walletRepository.save(any(WalletEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WalletResponse result = walletService.addAccount("w1", request);

        assertEquals("w1", result.id());
        assertEquals(1, result.accounts().size());
        assertEquals("CHECKING", result.accounts().get(0).type());
        assertEquals("RON", result.accounts().get(0).currency());
        assertEquals(BigDecimal.valueOf(200), result.accounts().get(0).balance());
    }

    // Same type+currency already exists (case-insensitive) - throws error, nothing saved
    @Test
    void addAccount_duplicateTypeAndCurrency_throwsIllegalArgumentException() {
        AccountEntity existingAccount = account("a1", BigDecimal.valueOf(50), List.of());
        WalletEntity existing = wallet("w1", "user1", List.of(existingAccount));
        CreateAccountRequest request = new CreateAccountRequest("checking", "ron", BigDecimal.valueOf(100));

        when(walletRepository.findById("w1")).thenReturn(Optional.of(existing));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.addAccount("w1", request)
        );

        assertTrue(exception.getMessage().contains("already exists"));
        verify(walletRepository, never()).save(any(WalletEntity.class));
    }

    // INCOME of 30 on balance 100 - 130, no category needed, saves transaction
    @Test
    void createTransaction_income_validRequest_updatesBalanceAndAddsTransaction() {
        AccountEntity acc = account("a1", BigDecimal.valueOf(100), List.of());
        WalletEntity existing = wallet("w1", "user1", List.of(acc));
        TransactionRequest request = new TransactionRequest(TransactionType.INCOME, BigDecimal.valueOf(30), null, "Salary");

        when(walletRepository.findById("w1")).thenReturn(Optional.of(existing));
        when(walletRepository.save(any(WalletEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WalletResponse result = walletService.createTransaction("w1", "a1", request);

        assertEquals(BigDecimal.valueOf(130), result.accounts().get(0).balance());

        ArgumentCaptor<WalletEntity> captor = ArgumentCaptor.forClass(WalletEntity.class);
        verify(walletRepository).save(captor.capture());
        Transaction savedTx = captor.getValue().accounts().get(0).transactions().get(0);
        assertEquals(TransactionType.INCOME, savedTx.type());
        assertEquals("Salary", savedTx.description());
        assertNull(savedTx.categoryId());
    }

    // EXPENSE without category ID - throws error (category required for expenses)
    @Test
    void createTransaction_expenseMissingCategory_throwsIllegalArgumentException() {
        AccountEntity acc = account("a1", BigDecimal.valueOf(100), List.of());
        WalletEntity existing = wallet("w1", "user1", List.of(acc));
        TransactionRequest request = new TransactionRequest(TransactionType.EXPENSE, BigDecimal.valueOf(10), null, "Food");

        when(walletRepository.findById("w1")).thenReturn(Optional.of(existing));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.createTransaction("w1", "a1", request)
        );

        assertTrue(exception.getMessage().contains("Category ID is required"));
        verify(walletRepository, never()).save(any(WalletEntity.class));
    }

    // EXPENSE of 50 but only 20 in balance - throws "Insufficient funds"
    @Test
    void createTransaction_expenseInsufficientFunds_throwsIllegalArgumentException() {
        AccountEntity acc = account("a1", BigDecimal.valueOf(20), List.of());
        WalletEntity existing = wallet("w1", "user1", List.of(acc));
        TransactionRequest request = new TransactionRequest(TransactionType.EXPENSE, BigDecimal.valueOf(50), "cat1", "Laptop");

        when(walletRepository.findById("w1")).thenReturn(Optional.of(existing));
        when(categoryRepository.findByIdAndUserId("cat1", "user1")).thenReturn(Optional.of(new Category("cat1", "user1", "Shopping")));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.createTransaction("w1", "a1", request)
        );

        assertTrue(exception.getMessage().contains("Insufficient funds"));
        verify(walletRepository, never()).save(any(WalletEntity.class));
    }

    // EXPENSE with non-existent category - throws EntityNotFoundException
    @Test
    void createTransaction_expenseWithUnknownCategory_throwsEntityNotFoundException() {
        AccountEntity acc = account("a1", BigDecimal.valueOf(200), List.of());
        WalletEntity existing = wallet("w1", "user1", List.of(acc));
        TransactionRequest request = new TransactionRequest(TransactionType.EXPENSE, BigDecimal.valueOf(10), "cat-missing", "Food");

        when(walletRepository.findById("w1")).thenReturn(Optional.of(existing));
        when(categoryRepository.findByIdAndUserId("cat-missing", "user1")).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> walletService.createTransaction("w1", "a1", request)
        );

        assertTrue(exception.getMessage().contains("Category: cat-missing"));
    }

    // Already spent 80 of 100 account budget, new 30 would exceed it - throws error
    @Test
    void createTransaction_expenseExceedsAccountBudget_throwsIllegalArgumentException() {
        LocalDateTime now = LocalDateTime.now();
        Transaction existingExpense = transaction(
                "t1", "w1", "a1", "cat1", BigDecimal.valueOf(80), TransactionType.EXPENSE, now
        );
        AccountEntity acc = account("a1", BigDecimal.valueOf(500), List.of(existingExpense));
        WalletEntity existing = wallet("w1", "user1", List.of(acc));

        Budget accountBudget = new Budget(
                "b1",
                "user1",
                "w1",
                "a1",
                null,
                BigDecimal.valueOf(100),
                now.getMonthValue(),
                now.getYear()
        );

        TransactionRequest request = new TransactionRequest(TransactionType.EXPENSE, BigDecimal.valueOf(30), "cat1", "Food");

        when(walletRepository.findById("w1")).thenReturn(Optional.of(existing));
        when(categoryRepository.findByIdAndUserId("cat1", "user1")).thenReturn(Optional.of(new Category("cat1", "user1", "Food")));
        when(budgetRepository.findByWalletIdAndAccountIdAndCategoryIdIsNullAndMonthAndYear(eq("w1"), eq("a1"), anyInt(), anyInt()))
                .thenReturn(Optional.of(accountBudget));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.createTransaction("w1", "a1", request)
        );

        assertTrue(exception.getMessage().contains("Account budget exceeded"));
    }

     // Already spent 60 of 70 category budget, new 20 would exceed it - throws error
    @Test
    void createTransaction_expenseExceedsCategoryBudget_throwsIllegalArgumentException() {
        LocalDateTime now = LocalDateTime.now();
        Transaction existingExpense = transaction(
                "t1", "w1", "a1", "cat1", BigDecimal.valueOf(60), TransactionType.EXPENSE, now
        );
        AccountEntity acc = account("a1", BigDecimal.valueOf(500), List.of(existingExpense));
        WalletEntity existing = wallet("w1", "user1", List.of(acc));

        Budget categoryBudget = new Budget(
                "b2",
                "user1",
                "w1",
                "a1",
                "cat1",
                BigDecimal.valueOf(70),
                now.getMonthValue(),
                now.getYear()
        );

        TransactionRequest request = new TransactionRequest(TransactionType.EXPENSE, BigDecimal.valueOf(20), "cat1", "Food");

        when(walletRepository.findById("w1")).thenReturn(Optional.of(existing));
        when(categoryRepository.findByIdAndUserId("cat1", "user1")).thenReturn(Optional.of(new Category("cat1", "user1", "Food")));
        when(budgetRepository.findByWalletIdAndAccountIdAndCategoryIdIsNullAndMonthAndYear(eq("w1"), eq("a1"), anyInt(), anyInt()))
                .thenReturn(Optional.empty());
        when(budgetRepository.findByWalletIdAndAccountIdAndCategoryIdAndMonthAndYear(eq("w1"), eq("a1"), eq("cat1"), anyInt(), anyInt()))
                .thenReturn(Optional.of(categoryBudget));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.createTransaction("w1", "a1", request)
        );

        assertTrue(exception.getMessage().contains("Category budget exceeded"));
    }

    // Deposit 40 on balance 100 - 140, creates INCOME transaction with "Deposit" description
    @Test
    void deposit_validAmount_addsIncomeTransaction() {
        AccountEntity acc = account("a1", BigDecimal.valueOf(100), List.of());
        WalletEntity existing = wallet("w1", "user1", List.of(acc));

        when(walletRepository.findById("w1")).thenReturn(Optional.of(existing));
        when(walletRepository.save(any(WalletEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WalletResponse result = walletService.deposit("w1", "a1", new UpdateAccountBalanceRequest(BigDecimal.valueOf(40)));

        assertEquals(BigDecimal.valueOf(140), result.accounts().get(0).balance());

        ArgumentCaptor<WalletEntity> captor = ArgumentCaptor.forClass(WalletEntity.class);
        verify(walletRepository).save(captor.capture());
        Transaction tx = captor.getValue().accounts().get(0).transactions().get(0);
        assertEquals(TransactionType.INCOME, tx.type());
        assertEquals("Deposit", tx.description());
        verifyNoInteractions(categoryRepository);
    }


    // Withdraw 25 from balance 100 - 75, creates EXPENSE transaction with "Withdraw" description
    @Test
    void withdraw_validAmount_subtractsBalanceAndAddsExpenseTransaction() {
        AccountEntity acc = account("a1", BigDecimal.valueOf(100), List.of());
        WalletEntity existing = wallet("w1", "user1", List.of(acc));

        when(walletRepository.findById("w1")).thenReturn(Optional.of(existing));
        when(budgetRepository.findByWalletIdAndAccountIdAndCategoryIdIsNullAndMonthAndYear(eq("w1"), eq("a1"), anyInt(), anyInt()))
                .thenReturn(Optional.empty());
        when(walletRepository.save(any(WalletEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WalletResponse result = walletService.withdraw("w1", "a1", new UpdateAccountBalanceRequest(BigDecimal.valueOf(25)));

        assertEquals(BigDecimal.valueOf(75), result.accounts().get(0).balance());

        ArgumentCaptor<WalletEntity> captor = ArgumentCaptor.forClass(WalletEntity.class);
        verify(walletRepository).save(captor.capture());
        Transaction tx = captor.getValue().accounts().get(0).transactions().get(0);
        assertEquals(TransactionType.EXPENSE, tx.type());
        assertEquals("Withdraw", tx.description());
        assertNull(tx.categoryId());
    }

    // Withdraw 20 from balance of 10 - throws "Insufficient funds", nothing saved
    @Test
    void withdraw_withInsufficientFunds_throwsIllegalArgumentException() {
        AccountEntity acc = account("a1", BigDecimal.valueOf(10), List.of());
        WalletEntity existing = wallet("w1", "user1", List.of(acc));

        when(walletRepository.findById("w1")).thenReturn(Optional.of(existing));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.withdraw("w1", "a1", new UpdateAccountBalanceRequest(BigDecimal.valueOf(20)))
        );

        assertTrue(exception.getMessage().contains("Insufficient funds"));
        verify(walletRepository, never()).save(any(WalletEntity.class));
    }


    // Deletes wallet and all its related budgets
    @Test
    void deleteWallet_existingWallet_deletesWalletAndRelatedBudgets() {
        when(walletRepository.findById("w1")).thenReturn(Optional.of(wallet("w1", "user1", List.of())));
        doNothing().when(budgetRepository).deleteByWalletId("w1");

        walletService.deleteWallet("w1");

        verify(budgetRepository, times(1)).deleteByWalletId("w1");
        verify(walletRepository, times(1)).deleteById("w1");
    }

    // Wallet not found - throws EntityNotFoundException, nothing deleted
    @Test
    void deleteWallet_missingWallet_throwsEntityNotFoundException() {
        when(walletRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> walletService.deleteWallet("missing"));
        verify(walletRepository, never()).deleteById(any());
    }


    // Removes account a1 from wallet (a2 remains), also deletes budgets for a1
    @Test
    void deleteAccount_existingAccount_removesItAndDeletesRelatedBudgets() {
        AccountEntity a1 = account("a1", BigDecimal.valueOf(100), List.of());
        AccountEntity a2 = account("a2", BigDecimal.valueOf(200), List.of());
        WalletEntity existing = wallet("w1", "user1", List.of(a1, a2));

        when(walletRepository.findById("w1")).thenReturn(Optional.of(existing));
        when(walletRepository.save(any(WalletEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        walletService.deleteAccount("w1", "a1");

        verify(budgetRepository).deleteByWalletIdAndAccountId("w1", "a1");

        ArgumentCaptor<WalletEntity> captor = ArgumentCaptor.forClass(WalletEntity.class);
        verify(walletRepository).save(captor.capture());
        WalletEntity saved = captor.getValue();
        assertEquals(1, saved.accounts().size());
        assertEquals("a2", saved.accounts().get(0).id());
    }

    // Account not in wallet - throws EntityNotFoundException, no budgets deleted
    @Test
    void deleteAccount_missingAccount_throwsEntityNotFoundException() {
        AccountEntity a1 = account("a1", BigDecimal.valueOf(100), List.of());
        WalletEntity existing = wallet("w1", "user1", List.of(a1));

        when(walletRepository.findById("w1")).thenReturn(Optional.of(existing));

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> walletService.deleteAccount("w1", "a2")
        );

        assertTrue(exception.getMessage().contains("Account: a2"));
        verify(budgetRepository, never()).deleteByWalletIdAndAccountId(any(), any());
        verify(walletRepository, never()).save(any(WalletEntity.class));
    }


    // Fetches existing account - returns correct id and balance
    @Test
    void getAccount_existingAccount_returnsAccountResponse() {
        AccountEntity a1 = account("a1", BigDecimal.valueOf(150), List.of());
        WalletEntity existing = wallet("w1", "user1", List.of(a1));

        when(walletRepository.findById("w1")).thenReturn(Optional.of(existing));

        AccountResponse result = walletService.getAccount("w1", "a1");

        assertNotNull(result);
        assertEquals("a1", result.id());
        assertEquals(BigDecimal.valueOf(150), result.balance());
    }

    // Account not in wallet - throws EntityNotFoundException
    @Test
    void getAccount_missingAccount_throwsEntityNotFoundException() {
        WalletEntity existing = wallet("w1", "user1", List.of(account("a1", BigDecimal.valueOf(150), List.of())));
        when(walletRepository.findById("w1")).thenReturn(Optional.of(existing));

        assertThrows(EntityNotFoundException.class, () -> walletService.getAccount("w1", "a2"));
    }


    // Returns 2 mapped transactions (EXPENSE + INCOME) for existing account
    @Test
    void getAccountTransactions_existingAccount_returnsMappedTransactions() {
        Transaction t1 = transaction("t1", "w1", "a1", "cat1", BigDecimal.valueOf(20), TransactionType.EXPENSE, LocalDateTime.now());
        Transaction t2 = transaction("t2", "w1", "a1", null, BigDecimal.valueOf(50), TransactionType.INCOME, LocalDateTime.now());
        WalletEntity existing = wallet("w1", "user1", List.of(account("a1", BigDecimal.valueOf(300), List.of(t1, t2))));

        when(walletRepository.findById("w1")).thenReturn(Optional.of(existing));

        List<TransactionResponse> result = walletService.getAccountTransactions("w1", "a1");

        assertEquals(2, result.size());
        assertEquals("t1", result.get(0).id());
        assertEquals(TransactionType.EXPENSE, result.get(0).type());
        assertEquals("t2", result.get(1).id());
        assertEquals(TransactionType.INCOME, result.get(1).type());
    }

    // Account not in wallet - throws EntityNotFoundException
    @Test
    void getAccountTransactions_missingAccount_throwsEntityNotFoundException() {
        WalletEntity existing = wallet("w1", "user1", List.of(account("a1", BigDecimal.valueOf(100), List.of())));
        when(walletRepository.findById("w1")).thenReturn(Optional.of(existing));

        assertThrows(EntityNotFoundException.class, () -> walletService.getAccountTransactions("w1", "a2"));
    }
}