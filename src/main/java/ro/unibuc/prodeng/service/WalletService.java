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
import ro.unibuc.prodeng.request.UpdateAccountBalanceRequest;
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

    public WalletResponse getWalletByUserId(String userId) throws EntityNotFoundException {
        String safeUserId = Objects.requireNonNull(userId, "userId must not be null");

        WalletEntity wallet = walletRepository.findByUserId(safeUserId)
                .orElseThrow(() -> new EntityNotFoundException("Wallet for user: " + safeUserId));
        return toResponse(wallet);
    }

    public WalletResponse addAccount(String walletId, CreateAccountRequest request) throws EntityNotFoundException {
        String safeWalletId = Objects.requireNonNull(walletId, "walletId must not be null");
        CreateAccountRequest safeRequest = Objects.requireNonNull(request, "request must not be null");

        WalletEntity wallet = walletRepository.findById(safeWalletId)
            .orElseThrow(() -> new EntityNotFoundException(walletId));

        boolean duplicate = wallet.accounts().stream()
                .anyMatch(a -> a.type().equalsIgnoreCase(safeRequest.type())
                        && a.currency().equalsIgnoreCase(safeRequest.currency()));
        if (duplicate) {
            throw new IllegalArgumentException(
                    "Account with type '" + safeRequest.type() + "' and currency '" + safeRequest.currency() + "' already exists in this wallet");
        }

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

    public WalletResponse deposit(String walletId, String accountId, UpdateAccountBalanceRequest request) throws EntityNotFoundException {
        String safeWalletId = Objects.requireNonNull(walletId, "walletId must not be null");
        String safeAccountId = Objects.requireNonNull(accountId, "accountId must not be null");

        WalletEntity wallet = walletRepository.findById(safeWalletId)
                .orElseThrow(() -> new EntityNotFoundException("Wallet: " + safeWalletId));

        boolean found = wallet.accounts().stream().anyMatch(a -> safeAccountId.equals(a.id()));
        if (!found) {
            throw new EntityNotFoundException("Account: " + safeAccountId);
        }

        List<AccountEntity> updatedAccounts = wallet.accounts().stream()
                .map(account -> safeAccountId.equals(account.id())
                        ? new AccountEntity(account.id(), account.type(), account.currency(),
                                account.balance().add(request.amount()), account.transactions())
                        : account)
                .toList();

        return toResponse(walletRepository.save(new WalletEntity(wallet.id(), wallet.userId(), updatedAccounts)));
    }

    public WalletResponse withdraw(String walletId, String accountId, UpdateAccountBalanceRequest request) throws EntityNotFoundException {
        String safeWalletId = Objects.requireNonNull(walletId, "walletId must not be null");
        String safeAccountId = Objects.requireNonNull(accountId, "accountId must not be null");

        WalletEntity wallet = walletRepository.findById(safeWalletId)
                .orElseThrow(() -> new EntityNotFoundException("Wallet: " + safeWalletId));

        AccountEntity target = wallet.accounts().stream()
                .filter(a -> safeAccountId.equals(a.id()))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Account: " + safeAccountId));

        if (target.balance().compareTo(request.amount()) < 0) {
            throw new IllegalArgumentException(
                    "Insufficient funds: balance is " + target.balance() + ", requested " + request.amount());
        }

        List<AccountEntity> updatedAccounts = wallet.accounts().stream()
                .map(account -> safeAccountId.equals(account.id())
                        ? new AccountEntity(account.id(), account.type(), account.currency(),
                                account.balance().subtract(request.amount()), account.transactions())
                        : account)
                .toList();

        return toResponse(walletRepository.save(new WalletEntity(wallet.id(), wallet.userId(), updatedAccounts)));
    }

    public void deleteWallet(String id) throws EntityNotFoundException {
        String walletId = Objects.requireNonNull(id, "id must not be null");

        if (!walletRepository.existsById(walletId)) {
            throw new EntityNotFoundException("Wallet: " + walletId);
        }

        walletRepository.deleteById(walletId);
    }

    public void deleteAccount(String walletId, String accountId) throws EntityNotFoundException {
        String safeWalletId = Objects.requireNonNull(walletId, "walletId must not be null");
        String safeAccountId = Objects.requireNonNull(accountId, "accountId must not be null");

        WalletEntity wallet = walletRepository.findById(safeWalletId)
                .orElseThrow(() -> new EntityNotFoundException("Wallet: " + safeWalletId));

        List<AccountEntity> updatedAccounts = new ArrayList<>(wallet.accounts());
        boolean removed = updatedAccounts.removeIf(account -> safeAccountId.equals(account.id()));

        if (!removed) {
            throw new EntityNotFoundException("Account: " + safeAccountId);
        }

        WalletEntity updatedWallet = new WalletEntity(
                wallet.id(),
                wallet.userId(),
                updatedAccounts
        );

        walletRepository.save(updatedWallet);
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
