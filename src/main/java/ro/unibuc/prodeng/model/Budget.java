package ro.unibuc.prodeng.model;

import java.math.BigDecimal;
import org.springframework.data.annotation.Id;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class Budget {
    @Id
    private String id;

    @NotBlank
    private String userId;
    
    private String categoryId;
    
    @NotNull
    @Positive(message = "Amount limit must be a positive value" )
    private BigDecimal amount_limit;
    
    @Min(1) @Max(12)
    private int month;
    
    @Min(2026)
    private int year;
}
