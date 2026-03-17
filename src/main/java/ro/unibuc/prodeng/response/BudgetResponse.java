package ro.unibuc.prodeng.response;

import java.math.BigDecimal;

public record BudgetResponse(
    String id,
    String userId,
    String walletId,
    String accountId,
    String categoryId,
    BigDecimal limitAmount,
    int month,
    int year
) {}
