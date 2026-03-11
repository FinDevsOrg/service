package ro.unibuc.prodeng.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import ro.unibuc.prodeng.model.WalletEntity;

@Repository
public interface WalletRepository extends MongoRepository<WalletEntity, String> {

    Optional<WalletEntity> findByUserId(String userId);
    boolean existsByUserId(String userId);
}
