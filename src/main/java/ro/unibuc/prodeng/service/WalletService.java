package ro.unibuc.prodeng.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ro.unibuc.prodeng.exception.EntityNotFoundException;
import ro.unibuc.prodeng.model.AccountEntity;
import ro.unibuc.prodeng.model.Budget;
import ro.unibuc.prodeng.model.Transaction;
import ro.unibuc.prodeng.model.TransactionType;
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

@Service
public class WalletService {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserService userService;

    public WalletResponse createWallet(String userId) throws EntityNotFoundException {
        String safeUserId = Objects.requireNonNull(userId, "userId must not be null");

        userService.getUserEntityById(safeUserId);

        if (walletRepository.existsByUserId(safeUserId)) {
            throw new IllegalArgumentException("Wallet already exists for user: " + safeUserId);
        }

        WalletEntity wallet = new WalletEntity(
                null,
                safeUserId,
                List.of()
        );

        WalletEntity saved = walletRepository.save(wallet);
        return toResponse(saved);
    }

    public WalletResponse getWalletById(String id) throws EntityNotFoundException {
        String walletId = Objects.requireNonNull(id, "id must not be null");

        WalletEntity wallet = findWalletById(walletId);
        return toResponse(wallet);
    }

    public WalletResponse getWalletByUserId(String userId) throws EntityNotFoundException {
        String safeUserId = Objects.requireNonNull(userId, "userId must not be null");

        WalletEntity wallet = walletRepository.findByUserId(safeUserId)
                .orElseThrow(() -> new EntityNotFoundException("Wallet for user: " + safeUserId));
        return toResponse(wallet);
    }

    public WalletResponse addAccount(String walletId, CreateAccountRequest request) throws EntityNotFoundException {
        String safeWalletId = Objects.requireNonNull(walletId, "walletId must not be null");
        CreateAccountRequest safeRequest = Objects.requireNonNull(request, "request must not be null");

        WalletEntity wallet = findWalletById(safeWalletId);

        boolean duplicate = wallet.accounts().stream()
                .anyMatch(a -> a.type().equalsIgnoreCase(safeRequest.type())
                        && a.currency().equalsIgnoreCase(safeRequest.currency()));
        if (duplicate) {
            throw new IllegalArgumentException(
                    "Account with type '" + safeRequest.type() + "' and currency '" + safeRequest.currency()
                            + "' already exists in this wallet");
        }

        AccountEntity account = new AccountEntity(
                UUID.randomUUID().toString(),
                safeRequest.type(),
                safeRequest.currency(),
                safeRequest.initialBalance(),
                List.of()
        );

        List<AccountEntity> updatedAccounts = new ArrayList<>(wallet.accounts());
        updatedAccounts.add(account);

        WalletEntity updatedWallet = new WalletEntity(
                wallet.id(),
                wallet.userId(),
                updatedAccounts
        );

        WalletEntity saved = walletRepository.save(updatedWallet);
        return toResponse(saved);
    }

    public WalletResponse createTransaction(String walletId, String accountId, TransactionRequest request)
            throws EntityNotFoundException {
        TransactionRequest safeRequest = Objects.requireNonNull(request, "request must not be null");

        return applyTransaction(
                walletId,
                accountId,
                safeRequest.type(),
                safeRequest.amount(),
                safeRequest.categoryId(),
                safeRequest.description(),
                true
        );
    }

    public WalletResponse deposit(String walletId, String accountId, UpdateAccountBalanceRequest request)
            throws EntityNotFoundException {
        UpdateAccountBalanceRequest safeRequest = Objects.requireNonNull(request, "request must not be null");

        return applyTransaction(
                walletId,
                accountId,
                TransactionType.INCOME,
                safeRequest.amount(),
                null,
                "Deposit",
                false
        );
    }

    public WalletResponse withdraw(String walletId, String accountId, UpdateAccountBalanceRequest request)
            throws EntityNotFoundException {
        UpdateAccountBalanceRequest safeRequest = Objects.requireNonNull(request, "request must not be null");

        return applyTransaction(
                walletId,
                accountId,
                TransactionType.EXPENSE,
                safeRequest.amount(),
                null,
                "Withdraw",
                false
        );
    }

    public void deleteWallet(String id) throws EntityNotFoundException {
        String walletId = Objects.requireNonNull(id, "id must not be null");

        findWalletById(walletId);

        budgetRepository.deleteByWalletId(walletId);
        walletRepository.deleteById(walletId);
    }

    public void deleteAccount(String walletId, String accountId) throws EntityNotFoundException {
        String safeWalletId = Objects.requireNonNull(walletId, "walletId must not be null");
        String safeAccountId = Objects.requireNonNull(accountId, "accountId must not be null");

        WalletEntity wallet = findWalletById(safeWalletId);

        List<AccountEntity> updatedAccounts = new ArrayList<>(wallet.accounts());
        boolean removed = updatedAccounts.removeIf(account -> safeAccountId.equals(account.id()));

        if (!removed) {
            throw new EntityNotFoundException("Account: " + safeAccountId);
        }

        budgetRepository.deleteByWalletIdAndAccountId(safeWalletId, safeAccountId);

        WalletEntity updatedWallet = new WalletEntity(
                wallet.id(),
                wallet.userId(),
                updatedAccounts
        );

        walletRepository.save(updatedWallet);
    }

    public AccountResponse getAccount(String walletId, String accountId) throws EntityNotFoundException {
        String safeWalletId = Objects.requireNonNull(walletId, "walletId must not be null");
        String safeAccountId = Objects.requireNonNull(accountId, "accountId must not be null");

        WalletEntity wallet = findWalletById(safeWalletId);
        AccountEntity account = wallet.accounts().stream()
                .filter(a -> safeAccountId.equals(a.id()))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Account: " + safeAccountId));

        return toAccountResponse(account);
    }
    
    public List<TransactionResponse> getAccountTransactions(String walletId, String accountId) throws EntityNotFoundException {
        String safeWalletId = Objects.requireNonNull(walletId, "walletId must not be null");
        String safeAccountId = Objects.requireNonNull(accountId, "accountId must not be null");

        WalletEntity wallet = findWalletById(safeWalletId);
        AccountEntity account = wallet.accounts().stream()
                .filter(a -> safeAccountId.equals(a.id()))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Account: " + safeAccountId));

        return account.transactions().stream()
                .map(this::toTransactionResponse)
                .toList();
    }
    
    private TransactionResponse toTransactionResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.id(),
                transaction.walletId(),
                transaction.accountId(),
                transaction.categoryId(),
                transaction.amount(),
                transaction.type(),
                transaction.date(),
                transaction.description()
        );
    }

    private WalletResponse applyTransaction(
            String walletId,
            String accountId,
            TransactionType type,
            BigDecimal amount,
            String categoryId,
            String description,
            boolean requireCategoryForExpense
    ) throws EntityNotFoundException {
        String safeWalletId = Objects.requireNonNull(walletId, "walletId must not be null");
        String safeAccountId = Objects.requireNonNull(accountId, "accountId must not be null");
        TransactionType safeType = Objects.requireNonNull(type, "type must not be null");
        BigDecimal safeAmount = Objects.requireNonNull(amount, "amount must not be null");
        String safeCategoryId = normalizeCategoryId(categoryId);

        if (safeAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be > 0");
        }

        WalletEntity wallet = findWalletById(safeWalletId);
        AccountEntity account = wallet.accounts().stream()
                .filter(a -> safeAccountId.equals(a.id()))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Account: " + safeAccountId));

        if (safeCategoryId != null) {
            categoryRepository.findByIdAndUserId(safeCategoryId, wallet.userId())
                    .orElseThrow(() -> new EntityNotFoundException("Category: " + safeCategoryId));
        }

        LocalDateTime now = LocalDateTime.now();
        int month = now.getMonthValue();
        int year = now.getYear();

        if (safeType == TransactionType.EXPENSE) {
            if (requireCategoryForExpense && safeCategoryId == null) {
                throw new IllegalArgumentException("Category ID is required for expense transactions");
            }

            if (account.balance().compareTo(safeAmount) < 0) {
                throw new IllegalArgumentException(
                        "Insufficient funds: balance is " + account.balance() + ", requested " + safeAmount);
            }

            validateAccountBudgetLimit(wallet.id(), safeAccountId, account, safeAmount, month, year);

            if (safeCategoryId != null) {
                validateCategoryBudgetLimit(wallet.id(), safeAccountId, safeCategoryId, account, safeAmount, month, year);
            }
        }

        BigDecimal updatedBalance = safeType == TransactionType.INCOME
                ? account.balance().add(safeAmount)
                : account.balance().subtract(safeAmount);

        String normalizedDescription = description == null || description.isBlank() ? null : description.trim();

        Transaction transaction = new Transaction(
                UUID.randomUUID().toString(),
                safeWalletId,
                safeAccountId,
                safeCategoryId,
                safeAmount,
                safeType,
                now,
                normalizedDescription,
                wallet.userId()
        );

        List<Transaction> updatedTransactions = new ArrayList<>(account.transactions());
        updatedTransactions.add(transaction);

        List<AccountEntity> updatedAccounts = wallet.accounts().stream()
                .map(current -> safeAccountId.equals(current.id())
                        ? new AccountEntity(
                                current.id(),
                                current.type(),
                                current.currency(),
                                updatedBalance,
                                updatedTransactions
                        )
                        : current)
                .toList();

        WalletEntity saved = walletRepository.save(new WalletEntity(wallet.id(), wallet.userId(), updatedAccounts));
        return toResponse(saved);
    }

    private void validateAccountBudgetLimit(
            String walletId,
            String accountId,
            AccountEntity account,
            BigDecimal amount,
            int month,
            int year
    ) {
        Optional<Budget> accountBudget = budgetRepository.findByWalletIdAndAccountIdAndCategoryIdIsNullAndMonthAndYear(
                walletId,
                accountId,
                month,
                year
        );

        if (accountBudget.isEmpty()) {
            return;
        }

        BigDecimal spent = sumExpensesForPeriod(account.transactions(), month, year, null);
        BigDecimal projectedSpent = spent.add(amount);

        if (projectedSpent.compareTo(accountBudget.get().limitAmount()) > 0) {
            throw new IllegalArgumentException(
                    "Account budget exceeded: limit is " + accountBudget.get().limitAmount()
                            + ", spent " + spent + ", requested " + amount);
        }
    }

    private void validateCategoryBudgetLimit(
            String walletId,
            String accountId,
            String categoryId,
            AccountEntity account,
            BigDecimal amount,
            int month,
            int year
    ) {
        Optional<Budget> categoryBudget = budgetRepository.findByWalletIdAndAccountIdAndCategoryIdAndMonthAndYear(
                walletId,
                accountId,
                categoryId,
                month,
                year
        );

        if (categoryBudget.isEmpty()) {
            return;
        }

        BigDecimal spent = sumExpensesForPeriod(account.transactions(), month, year, categoryId);
        BigDecimal projectedSpent = spent.add(amount);

        if (projectedSpent.compareTo(categoryBudget.get().limitAmount()) > 0) {
            throw new IllegalArgumentException(
                    "Category budget exceeded: limit is " + categoryBudget.get().limitAmount()
                            + ", spent " + spent + ", requested " + amount);
        }
    }

    private BigDecimal sumExpensesForPeriod(List<Transaction> transactions, int month, int year, String categoryId) {
        return transactions.stream()
                .filter(transaction -> transaction.type() == TransactionType.EXPENSE)
                .filter(transaction -> transaction.date() != null)
                .filter(transaction -> transaction.date().getMonthValue() == month && transaction.date().getYear() == year)
                .filter(transaction -> categoryId == null || categoryId.equals(transaction.categoryId()))
                .map(Transaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String normalizeCategoryId(String categoryId) {
        if (categoryId == null) {
            return null;
        }
        String trimmed = categoryId.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private WalletEntity findWalletById(String walletId) throws EntityNotFoundException {
        String safeWalletId = Objects.requireNonNull(walletId, "walletId must not be null");

        return walletRepository.findById(safeWalletId)
                .orElseThrow(() -> new EntityNotFoundException("Wallet: " + safeWalletId));
    }

    private WalletResponse toResponse(WalletEntity wallet) {
        List<AccountResponse> accounts = wallet.accounts().stream()
                .map(this::toAccountResponse)
                .toList();

        return new WalletResponse(
                wallet.id(),
                wallet.userId(),
                accounts
        );
    }

    private AccountResponse toAccountResponse(AccountEntity account) {
        return new AccountResponse(
                account.id(),
                account.type(),
                account.currency(),
                account.balance()
        );
    }
}
