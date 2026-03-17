package ro.unibuc.prodeng.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SetBudgetRequest(
    String categoryId,

    @NotNull(message = "Amount limit is required")
    @Positive(message = "Amount limit must be a positive value")
    BigDecimal amountLimit,

    @Min(1) @Max(12)
    int month,

    @Min(value = 2020, message = "Year must be >= 2020")
    int year
) {}
