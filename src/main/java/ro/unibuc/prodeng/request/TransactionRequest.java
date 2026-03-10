package ro.unibuc.prodeng.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class TransactionRequest {
    @NotBlank(message = "Wallet ID is required")
    private String walletId;

    @NotBlank(message = "Category ID is required")
    private String categoryId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be a positive value")
    private BigDecimal amount;

    private String description;
}
