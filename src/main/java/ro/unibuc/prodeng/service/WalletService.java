package ro.unibuc.prodeng.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ro.unibuc.prodeng.exception.EntityNotFoundException;
import ro.unibuc.prodeng.model.AccountEntity;
import ro.unibuc.prodeng.model.WalletEntity;
import ro.unibuc.prodeng.repository.WalletRepository;
import ro.unibuc.prodeng.request.CreateAccountRequest;
import ro.unibuc.prodeng.response.AccountResponse;
import ro.unibuc.prodeng.response.WalletResponse;

@Service
public class WalletService {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private UserService userService;

    public WalletResponse createWallet(String userId) throws EntityNotFoundException {
        String safeUserId = Objects.requireNonNull(userId, "userId must not be null");

        userService.getUserEntityById(safeUserId);

        if (walletRepository.existsByUserId(safeUserId)) {
            throw new IllegalArgumentException("Wallet already exists for user: " + safeUserId);
        }

        WalletEntity wallet = new WalletEntity(
                null,
                safeUserId,
                List.of()
        );

        WalletEntity saved = walletRepository.save(wallet);
        return toResponse(saved);
    }

    public WalletResponse getWalletById(String id) throws EntityNotFoundException {
        String walletId = Objects.requireNonNull(id, "id must not be null");

        WalletEntity wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new EntityNotFoundException(id));
        return toResponse(wallet);
    }

        public WalletResponse addAccount(String walletId, CreateAccountRequest request) throws EntityNotFoundException {
        String safeWalletId = Objects.requireNonNull(walletId, "walletId must not be null");
        CreateAccountRequest safeRequest = Objects.requireNonNull(request, "request must not be null");

        WalletEntity wallet = walletRepository.findById(safeWalletId)
            .orElseThrow(() -> new EntityNotFoundException(walletId));

        AccountEntity account = new AccountEntity(
            UUID.randomUUID().toString(),
            safeRequest.type(),
            safeRequest.currency(),
            safeRequest.initialBalance(),
            List.of()
        );

        List<AccountEntity> updatedAccounts = new ArrayList<>(wallet.accounts());
        updatedAccounts.add(account);

        WalletEntity updatedWallet = new WalletEntity(
            wallet.id(),
            wallet.userId(),
            updatedAccounts
        );

        WalletEntity saved = walletRepository.save(updatedWallet);
        return toResponse(saved);
        }

    private WalletResponse toResponse(WalletEntity wallet) {
        List<AccountResponse> accounts = wallet.accounts().stream()
                .map(this::toAccountResponse)
                .toList();

        return new WalletResponse(
                wallet.id(),
                wallet.userId(),
                accounts
        );
    }

    private AccountResponse toAccountResponse(AccountEntity account) {
        return new AccountResponse(
                account.id(),
                account.type(),
                account.currency(),
                account.balance()
        );
    }
}
