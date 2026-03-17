package ro.unibuc.prodeng.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import ro.unibuc.prodeng.model.TransactionType;

public record TransactionResponse(
    String id,
    String walletId,
    String accountId,
    String categoryId,
    BigDecimal amount,
    TransactionType type,
    LocalDateTime date,
    String description
) {}