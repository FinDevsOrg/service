package ro.unibuc.prodeng.service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ro.unibuc.prodeng.exception.EntityNotFoundException;
import ro.unibuc.prodeng.model.Transaction;
import ro.unibuc.prodeng.model.WalletEntity;
import ro.unibuc.prodeng.repository.WalletRepository;
import ro.unibuc.prodeng.response.TransactionResponse;

@Service
public class TransactionService {

    @Autowired
    private WalletRepository walletRepository;

    public List<TransactionResponse> filterTransactions(
            String walletId,
            String accountId,
            String categoryId,
            String userId) throws EntityNotFoundException {

        Stream<Transaction> transactions = walletRepository.findAll().stream()
                .filter(wallet -> userId == null || wallet.userId().equals(userId))
                .filter(wallet -> walletId == null || wallet.id().equals(walletId))
                .flatMap(wallet -> wallet.accounts().stream()
                        .filter(account -> accountId == null || account.id().equals(accountId))
                        .flatMap(account -> account.transactions().stream()));

        return transactions
                .filter(tx -> categoryId == null || categoryId.equals(tx.categoryId()))
                .map(this::toResponse)
                .toList();
    }

    private TransactionResponse toResponse(Transaction transaction) {
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
}