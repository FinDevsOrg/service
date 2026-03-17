package ro.unibuc.prodeng.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import ro.unibuc.prodeng.exception.EntityNotFoundException;
import ro.unibuc.prodeng.request.CreateAccountRequest;
import ro.unibuc.prodeng.request.TransactionRequest;
import ro.unibuc.prodeng.request.UpdateAccountBalanceRequest;
import ro.unibuc.prodeng.response.WalletResponse;
import ro.unibuc.prodeng.service.WalletService;
import ro.unibuc.prodeng.response.AccountResponse;
import ro.unibuc.prodeng.response.TransactionResponse;
import java.util.List;

@RestController
@RequestMapping("/api/wallets")
public class WalletController {

    @Autowired
    private WalletService walletService;

    @PostMapping("/user/{userId}")
    public ResponseEntity<WalletResponse> createWallet(@PathVariable String userId)
            throws EntityNotFoundException {
        WalletResponse wallet = walletService.createWallet(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(wallet);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<WalletResponse> getWalletByUserId(@PathVariable String userId)
            throws EntityNotFoundException {
        WalletResponse wallet = walletService.getWalletByUserId(userId);
        return ResponseEntity.ok(wallet);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WalletResponse> getWalletById(@PathVariable String id)
            throws EntityNotFoundException {
        WalletResponse wallet = walletService.getWalletById(id);
        return ResponseEntity.ok(wallet);
    }

    @GetMapping("/{walletId}/accounts/{accountId}")
public ResponseEntity<AccountResponse> getAccount(
        @PathVariable String walletId,
        @PathVariable String accountId) throws EntityNotFoundException {
    AccountResponse account = walletService.getAccount(walletId, accountId);
    return ResponseEntity.ok(account);
}

@GetMapping("/{walletId}/accounts/{accountId}/transactions")
public ResponseEntity<List<TransactionResponse>> getAccountTransactions(
        @PathVariable String walletId,
        @PathVariable String accountId) throws EntityNotFoundException {
    List<TransactionResponse> transactions = walletService.getAccountTransactions(walletId, accountId);
    return ResponseEntity.ok(transactions);
}

    @PostMapping("/{id}/accounts")
    public ResponseEntity<WalletResponse> addAccount(@PathVariable String id,@Valid @RequestBody CreateAccountRequest request) throws EntityNotFoundException {
        WalletResponse wallet = walletService.addAccount(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(wallet);
    }

    @PostMapping("/{walletId}/accounts/{accountId}/transactions")
    public ResponseEntity<WalletResponse> createTransaction(@PathVariable String walletId, @PathVariable String accountId, @Valid @RequestBody TransactionRequest request) throws EntityNotFoundException {
        WalletResponse wallet = walletService.createTransaction(walletId, accountId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(wallet);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWallet(@PathVariable String id) throws EntityNotFoundException {
        walletService.deleteWallet(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{walletId}/accounts/{accountId}")
    public ResponseEntity<Void> deleteAccount(
            @PathVariable String walletId,
            @PathVariable String accountId) throws EntityNotFoundException {
        walletService.deleteAccount(walletId, accountId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{walletId}/accounts/{accountId}/deposit")
    public ResponseEntity<WalletResponse> deposit(
            @PathVariable String walletId,
            @PathVariable String accountId,
            @Valid @RequestBody UpdateAccountBalanceRequest request) throws EntityNotFoundException {
        WalletResponse wallet = walletService.deposit(walletId, accountId, request);
        return ResponseEntity.ok(wallet);
    }

    @PatchMapping("/{walletId}/accounts/{accountId}/withdraw")
    public ResponseEntity<WalletResponse> withdraw(
            @PathVariable String walletId,
            @PathVariable String accountId,
            @Valid @RequestBody UpdateAccountBalanceRequest request) throws EntityNotFoundException {
        WalletResponse wallet = walletService.withdraw(walletId, accountId, request);
        return ResponseEntity.ok(wallet);
    }
}
