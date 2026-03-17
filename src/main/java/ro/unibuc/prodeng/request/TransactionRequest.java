package ro.unibuc.prodeng.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import ro.unibuc.prodeng.model.TransactionType;

public record TransactionRequest(
    @NotNull(message = "Transaction type is required")
    TransactionType type,

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be a positive value")
    BigDecimal amount,

    String categoryId,
    String description
) {}
