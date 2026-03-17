package ro.unibuc.prodeng.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import ro.unibuc.prodeng.model.Category;

@Repository
public interface CategoryRepository extends MongoRepository<Category, String> {

    List<Category> findByUserId(String userId);

    boolean existsByUserIdAndNameIgnoreCase(String userId, String name);

    Optional<Category> findByIdAndUserId(String id, String userId);

    void deleteByUserId(String userId);
}
