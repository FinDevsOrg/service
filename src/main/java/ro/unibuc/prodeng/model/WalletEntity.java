package ro.unibuc.prodeng.model;

import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "wallets")
public record WalletEntity(
    @Id String id,
    @Indexed(unique = true) String userId,
    List<AccountEntity> accounts
) {
    public WalletEntity {
        accounts = accounts == null ? List.of() : List.copyOf(accounts);
    }
}
