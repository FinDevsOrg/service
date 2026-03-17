package ro.unibuc.prodeng.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateBudgetRequest(
    @NotNull(message = "Amount limit is required")
    @Positive(message = "Amount limit must be a positive value")
    BigDecimal amountLimit
) {}