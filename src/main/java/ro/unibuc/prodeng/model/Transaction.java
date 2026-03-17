package ro.unibuc.prodeng.model;

import org.springframework.data.annotation.Id;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Transaction(
    @Id
    String id,
    String walletId,
    String accountId,
    String categoryId,
    BigDecimal amount,
    TransactionType type,
    LocalDateTime date,
    String description,
    String userId
) {
    public Transaction {
        date = date == null ? LocalDateTime.now() : date;
        categoryId = categoryId == null || categoryId.isBlank() ? null : categoryId;
    }
}