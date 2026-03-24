package ro.unibuc.prodeng.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ro.unibuc.prodeng.model.AccountEntity;
import ro.unibuc.prodeng.model.Transaction;
import ro.unibuc.prodeng.model.TransactionType;
import ro.unibuc.prodeng.model.WalletEntity;
import ro.unibuc.prodeng.repository.WalletRepository;
import ro.unibuc.prodeng.response.TransactionResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class TransactionServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private TransactionService transactionService;

    private final LocalDateTime NOW = LocalDateTime.of(2024, 1, 1, 12, 0);

    private Transaction buildTransaction(String id, String walletId, String accountId, String categoryId, String userId) {
        return new Transaction(id, walletId, accountId, categoryId, BigDecimal.TEN, TransactionType.INCOME, NOW, "desc", userId);
    }

    private WalletEntity buildWalletWithTransactions(String walletId, String userId, List<Transaction> transactions) {
        AccountEntity account = new AccountEntity("acc1", "CHECKING", "USD", BigDecimal.valueOf(1000), transactions);
        return new WalletEntity(walletId, userId, List.of(account));
    }

    @Test
    void filterTransactions_noFilters_returnsAllTransactions() throws Exception {
        Transaction tx1 = buildTransaction("tx1", "w1", "acc1", "cat1", "user1");
        Transaction tx2 = buildTransaction("tx2", "w2", "acc1", "cat2", "user2");
        when(walletRepository.findAll()).thenReturn(List.of(
                buildWalletWithTransactions("w1", "user1", List.of(tx1)),
                buildWalletWithTransactions("w2", "user2", List.of(tx2))
        ));

        List<TransactionResponse> result = transactionService.filterTransactions(null, null, null, null);

        assertEquals(2, result.size());
    }

    @Test
    void filterTransactions_withUserId_returnsOnlyThatUsersTransactions() throws Exception {
        Transaction tx1 = buildTransaction("tx1", "w1", "acc1", "cat1", "user1");
        Transaction tx2 = buildTransaction("tx2", "w2", "acc1", "cat2", "user2");
        when(walletRepository.findAll()).thenReturn(List.of(
                buildWalletWithTransactions("w1", "user1", List.of(tx1)),
                buildWalletWithTransactions("w2", "user2", List.of(tx2))
        ));

        List<TransactionResponse> result = transactionService.filterTransactions(null, null, null, "user1");

        assertEquals(1, result.size());
        assertEquals("tx1", result.get(0).id());
    }

    @Test
    void filterTransactions_withWalletId_returnsOnlyThatWalletsTransactions() throws Exception {
        Transaction tx1 = buildTransaction("tx1", "w1", "acc1", "cat1", "user1");
        Transaction tx2 = buildTransaction("tx2", "w2", "acc1", "cat2", "user2");
        when(walletRepository.findAll()).thenReturn(List.of(
                buildWalletWithTransactions("w1", "user1", List.of(tx1)),
                buildWalletWithTransactions("w2", "user2", List.of(tx2))
        ));

        List<TransactionResponse> result = transactionService.filterTransactions("w2", null, null, null);

        assertEquals(1, result.size());
        assertEquals("tx2", result.get(0).id());
    }

    @Test
    void filterTransactions_withAccountId_returnsOnlyThatAccountsTransactions() throws Exception {
        Transaction tx1 = buildTransaction("tx1", "w1", "acc1", "cat1", "user1");
        Transaction tx2 = buildTransaction("tx2", "w1", "acc2", "cat1", "user1");
        AccountEntity acc1 = new AccountEntity("acc1", "CHECKING", "USD", BigDecimal.valueOf(1000), List.of(tx1));
        AccountEntity acc2 = new AccountEntity("acc2", "SAVINGS", "USD", BigDecimal.valueOf(500), List.of(tx2));
        WalletEntity wallet = new WalletEntity("w1", "user1", List.of(acc1, acc2));
        when(walletRepository.findAll()).thenReturn(List.of(wallet));

        List<TransactionResponse> result = transactionService.filterTransactions(null, "acc1", null, null);

        assertEquals(1, result.size());
        assertEquals("tx1", result.get(0).id());
    }

    @Test
    void filterTransactions_withCategoryId_returnsOnlyMatchingTransactions() throws Exception {
        Transaction tx1 = buildTransaction("tx1", "w1", "acc1", "cat1", "user1");
        Transaction tx2 = buildTransaction("tx2", "w1", "acc1", "cat2", "user1");
        AccountEntity account = new AccountEntity("acc1", "CHECKING", "USD", BigDecimal.valueOf(1000), List.of(tx1, tx2));
        WalletEntity wallet = new WalletEntity("w1", "user1", List.of(account));
        when(walletRepository.findAll()).thenReturn(List.of(wallet));

        List<TransactionResponse> result = transactionService.filterTransactions(null, null, "cat1", null);

        assertEquals(1, result.size());
        assertEquals("tx1", result.get(0).id());
    }

    @Test
    void filterTransactions_noWallets_returnsEmptyList() throws Exception {
        when(walletRepository.findAll()).thenReturn(List.of());

        List<TransactionResponse> result = transactionService.filterTransactions(null, null, null, null);

        assertTrue(result.isEmpty());
    }
}
