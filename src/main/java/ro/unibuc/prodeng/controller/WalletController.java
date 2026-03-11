package ro.unibuc.prodeng.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import ro.unibuc.prodeng.exception.EntityNotFoundException;
import ro.unibuc.prodeng.request.CreateAccountRequest;
import ro.unibuc.prodeng.response.WalletResponse;
import ro.unibuc.prodeng.service.WalletService;

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

    @GetMapping("/{id}")
    public ResponseEntity<WalletResponse> getWalletById(@PathVariable String id)
            throws EntityNotFoundException {
        WalletResponse wallet = walletService.getWalletById(id);
        return ResponseEntity.ok(wallet);
    }

    @PostMapping("/{id}/accounts")
    public ResponseEntity<WalletResponse> addAccount(
            @PathVariable String id,
            @Valid @RequestBody CreateAccountRequest request) throws EntityNotFoundException {
        WalletResponse wallet = walletService.addAccount(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(wallet);
    }
}
