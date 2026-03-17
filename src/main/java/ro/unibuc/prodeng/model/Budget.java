package ro.unibuc.prodeng.model;

import java.math.BigDecimal;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "budgets")
public record Budget(
    @Id
    String id,
    String userId,
    String walletId,
    String accountId,
    String categoryId,
    BigDecimal limitAmount,
    int month,
    int year
) {
    public Budget {
        categoryId = categoryId == null || categoryId.isBlank() ? null : categoryId;
    }
}
