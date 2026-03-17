package ro.unibuc.prodeng.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import ro.unibuc.prodeng.exception.EntityNotFoundException;
import ro.unibuc.prodeng.request.SetBudgetRequest;
import ro.unibuc.prodeng.response.BudgetResponse;
import ro.unibuc.prodeng.service.BudgetService;

@RestController
@RequestMapping("/api/wallets/{walletId}/accounts/{accountId}/budgets")
public class BudgetController {

    @Autowired
    private BudgetService budgetService;

    @PostMapping
    public ResponseEntity<BudgetResponse> setBudget(
            @PathVariable String walletId,
            @PathVariable String accountId,
            @Valid @RequestBody SetBudgetRequest request) throws EntityNotFoundException {
        BudgetResponse budget = budgetService.setBudget(walletId, accountId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(budget);
    }

    @GetMapping
    public ResponseEntity<List<BudgetResponse>> getBudgets(
            @PathVariable String walletId,
            @PathVariable String accountId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) throws EntityNotFoundException {
        List<BudgetResponse> budgets = budgetService.getBudgets(walletId, accountId, month, year);
        return ResponseEntity.ok(budgets);
    }

    @DeleteMapping("/{budgetId}")
    public ResponseEntity<Void> deleteBudget(
            @PathVariable String walletId,
            @PathVariable String accountId,
            @PathVariable String budgetId) throws EntityNotFoundException {
        budgetService.deleteBudget(walletId, accountId, budgetId);
        return ResponseEntity.noContent().build();
    }
}
