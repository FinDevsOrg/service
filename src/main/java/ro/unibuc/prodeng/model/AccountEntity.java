package ro.unibuc.prodeng.model;

import java.math.BigDecimal;

public record AccountEntity(
    String id,
    String type,
    String currency,
    BigDecimal balance
) {}