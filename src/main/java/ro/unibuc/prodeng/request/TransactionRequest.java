package ro.unibuc.prodeng.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public class TransactionRequest {
    @NotBlank(message = "Wallet ID is required")
    private String walletId;

    @NotBlank(message = "Category ID is required")
    private String categoryId;

    @NotBlank(message = "Amount is required")
    @Positive
    private BigDecimal amount;

    private String description;
}
