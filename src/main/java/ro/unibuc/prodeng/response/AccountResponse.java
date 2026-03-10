package ro.unibuc.prodeng.response;

import java.math.BigDecimal;

public record AccountResponse(
    String id,
    String type,
    String currency,
    BigDecimal balance
) {}
