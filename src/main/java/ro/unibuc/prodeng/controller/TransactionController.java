package ro.unibuc.prodeng.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ro.unibuc.prodeng.exception.EntityNotFoundException;
import ro.unibuc.prodeng.response.TransactionResponse;
import ro.unibuc.prodeng.service.TransactionService;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getTransactions(
            @RequestParam(required = false) String walletId,
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String userId) throws EntityNotFoundException {
        List<TransactionResponse> transactions = transactionService.filterTransactions(walletId, accountId, categoryId, userId);
        return ResponseEntity.ok(transactions);
    }
}