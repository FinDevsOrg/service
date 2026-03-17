package ro.unibuc.prodeng.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import ro.unibuc.prodeng.model.Budget;

@Repository
public interface BudgetRepository extends MongoRepository<Budget, String> {

    List<Budget> findByWalletIdAndAccountId(String walletId, String accountId);

    List<Budget> findByWalletIdAndAccountIdAndMonthAndYear(String walletId, String accountId, int month, int year);

    Optional<Budget> findByWalletIdAndAccountIdAndCategoryIdIsNullAndMonthAndYear(
            String walletId,
            String accountId,
            int month,
            int year
    );

    Optional<Budget> findByWalletIdAndAccountIdAndCategoryIdAndMonthAndYear(
            String walletId,
            String accountId,
            String categoryId,
            int month,
            int year
    );

    Optional<Budget> findByIdAndWalletIdAndAccountId(String id, String walletId, String accountId);

    boolean existsByCategoryId(String categoryId);

    void deleteByCategoryId(String categoryId);

    void deleteByWalletId(String walletId);

    void deleteByWalletIdAndAccountId(String walletId, String accountId);

    void deleteByUserId(String userId);
}
