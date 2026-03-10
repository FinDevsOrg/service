package ro.unibuc.prodeng.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public class SetBudgetRequest {
    @NotBlank(message = "Category ID is required")
    private String categoryId;

    @NotNull(message = "Amount limit is required")
    @Positive(message = "Amount limit must be a positive value")
    private BigDecimal amountLimit;

    @Min(1) @Max(12)
    private int month;

    private int year;
}
