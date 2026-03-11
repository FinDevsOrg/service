package ro.unibuc.prodeng.model;

import org.springframework.data.annotation.Id;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Transaction {
    @Id
    private String id;

    @NotNull(message = "Wallet ID is required")
    private String walletId;

    @NotNull(message = "Account ID is required")
    private String accountId;

    @NotNull(message = "Category ID is required")
    private String categoryId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be a positive value")
    private BigDecimal amount;

    @NotNull(message = "Transaction type is required")  
    private TransactionType type;

    private LocalDateTime date = LocalDateTime.now();

    private String description;

    @NotBlank(message = "User ID is required")
    private String userId;
}