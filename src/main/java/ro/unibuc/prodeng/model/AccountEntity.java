package ro.unibuc.prodeng.model;

import java.math.BigDecimal;
import java.util.List;

public record AccountEntity(
    String id,
    String type,
    String currency,
    BigDecimal balance,
    List<Transaction> transactions
) {
    public AccountEntity {
        transactions = transactions == null ? List.of() : List.copyOf(transactions);
    }
}