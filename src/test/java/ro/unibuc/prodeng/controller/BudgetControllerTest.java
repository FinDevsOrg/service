package ro.unibuc.prodeng.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import ro.unibuc.prodeng.exception.EntityNotFoundException;
import ro.unibuc.prodeng.exception.GlobalExceptionHandler;
import ro.unibuc.prodeng.request.SetBudgetRequest;
import ro.unibuc.prodeng.request.UpdateBudgetRequest;
import ro.unibuc.prodeng.response.BudgetResponse;
import ro.unibuc.prodeng.service.BudgetService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

//test pentru build

@ExtendWith(SpringExtension.class)
class BudgetControllerTest {

    @Mock
    private BudgetService budgetService;

    @InjectMocks
    private BudgetController budgetController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();
    
    // 2 sample budgets (budget1, budget2) and sample request objects are pre-defined

    private BudgetResponse budget1 = new BudgetResponse(
            "b1",
            "user1",
            "w1",
            "acc1",
            "cat1",
            BigDecimal.valueOf(100.00),
            3,
            2026
    );

    private BudgetResponse budget2 = new BudgetResponse(
            "b2",
            "user1",
            "w1",
            "acc1",
            "cat2",
            BigDecimal.valueOf(50.00),
            3,
            2026
    );

    private SetBudgetRequest setBudgetRequest = new SetBudgetRequest(
            "cat1",
            BigDecimal.valueOf(100.00),
            3,
            2026
    );

    private UpdateBudgetRequest updateBudgetRequest = new UpdateBudgetRequest(
            BigDecimal.valueOf(150.00)
    );

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(budgetController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ==========================================
    // setBudget (POST) Tests
    // ==========================================

    // Valid POST request to create a budget - expects 201 and all fields returned correctly
    @Test
    void setBudget_validRequest_returnsCreated() throws Exception {
        when(budgetService.setBudget(
                eq("w1"),
                eq("acc1"),
                any(SetBudgetRequest.class)
        )).thenReturn(budget1);

        mockMvc.perform(post("/api/wallets/w1/accounts/acc1/budgets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(setBudgetRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is("b1")))
                .andExpect(jsonPath("$.walletId", is("w1")))
                .andExpect(jsonPath("$.accountId", is("acc1")))
                .andExpect(jsonPath("$.categoryId", is("cat1")))
                .andExpect(jsonPath("$.limitAmount", is(100.00)))
                .andExpect(jsonPath("$.month", is(3)))
                .andExpect(jsonPath("$.year", is(2026)));

        verify(budgetService, times(1)).setBudget(eq("w1"), eq("acc1"), any(SetBudgetRequest.class));
    }

    // Wallet ID doesn't exist - expects 404 Not Found
    @Test
    void setBudget_invalidWallet_returnsNotFound() throws Exception {
        when(budgetService.setBudget(
                eq("unknown"),
                eq("acc1"),
                any(SetBudgetRequest.class)
        )).thenThrow(new EntityNotFoundException("Wallet: unknown"));

        mockMvc.perform(post("/api/wallets/unknown/accounts/acc1/budgets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(setBudgetRequest)))
                .andExpect(status().isNotFound());
    }

    // Account ID doesn't exist - expects 404 Not Found
    @Test
    void setBudget_invalidAccount_returnsNotFound() throws Exception {
        when(budgetService.setBudget(
                eq("w1"),
                eq("unknown"),
                any(SetBudgetRequest.class)
        )).thenThrow(new EntityNotFoundException("Account: unknown"));

        mockMvc.perform(post("/api/wallets/w1/accounts/unknown/budgets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(setBudgetRequest)))
                .andExpect(status().isNotFound());
    }

    // Category ID doesn't exist - expects 404 Not Found
    @Test
    void setBudget_invalidCategory_returnsNotFound() throws Exception {
        when(budgetService.setBudget(
                eq("w1"),
                eq("acc1"),
                any(SetBudgetRequest.class)
        )).thenThrow(new EntityNotFoundException("Category: unknown"));

        mockMvc.perform(post("/api/wallets/w1/accounts/acc1/budgets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(setBudgetRequest)))
                .andExpect(status().isNotFound());
    }

    // Missing amountLimit field in request body - expects 400 Bad Request (validation)
    @Test
    void setBudget_nullAmountLimit_returnsBadRequest() throws Exception {
        String invalidRequest = "{\"categoryId\": \"cat1\", \"month\": 3, \"year\": 2026}";

        mockMvc.perform(post("/api/wallets/w1/accounts/acc1/budgets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    // Negative amountLimit value - expects 400 Bad Request (validation)
    @Test
    void setBudget_negativeAmountLimit_returnsBadRequest() throws Exception {
        String invalidRequest = "{\"categoryId\": \"cat1\", \"amountLimit\": -50, \"month\": 3, \"year\": 2026}";

        mockMvc.perform(post("/api/wallets/w1/accounts/acc1/budgets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    // ==========================================
    // getBudgets (GET) Tests
    // ==========================================

    // GET all budgets without filters - expects 200 with both budgets returned
    @Test
    void getBudgets_noFilters_returnsAllBudgets() throws Exception {
        when(budgetService.getBudgets("w1", "acc1", null, null)).thenReturn(List.of(budget1, budget2));

        mockMvc.perform(get("/api/wallets/w1/accounts/acc1/budgets")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is("b1")))
                .andExpect(jsonPath("$[1].id", is("b2")));

        verify(budgetService, times(1)).getBudgets("w1", "acc1", null, null);
    }

    // GET budgets filtered by month and year query params - expects matching results
    @Test
    void getBudgets_withMonthAndYear_returnsFilteredBudgets() throws Exception {
        when(budgetService.getBudgets("w1", "acc1", 3, 2026)).thenReturn(List.of(budget1, budget2));

        mockMvc.perform(get("/api/wallets/w1/accounts/acc1/budgets")
                .param("month", "3")
                .param("year", "2026")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].month", is(3)))
                .andExpect(jsonPath("$[0].year", is(2026)));

        verify(budgetService, times(1)).getBudgets("w1", "acc1", 3, 2026);
    }

    // No budgets found - expects 200 with an empty list
    @Test
    void getBudgets_emptyResult_returnsEmptyList() throws Exception {
        when(budgetService.getBudgets("w1", "acc1", null, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/wallets/w1/accounts/acc1/budgets")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // GET budgets with unknown wallet - expects 404
    @Test
    void getBudgets_invalidWallet_returnsNotFound() throws Exception {
        when(budgetService.getBudgets("unknown", "acc1", null, null))
                .thenThrow(new EntityNotFoundException("Wallet: unknown"));

        mockMvc.perform(get("/api/wallets/unknown/accounts/acc1/budgets")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // GET budgets with unknown account - expects 404
    @Test
    void getBudgets_invalidAccount_returnsNotFound() throws Exception {
        when(budgetService.getBudgets("w1", "unknown", null, null))
                .thenThrow(new EntityNotFoundException("Account: unknown"));

        mockMvc.perform(get("/api/wallets/w1/accounts/unknown/budgets")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

     // ==========================================
    // getBudget (GET /{budgetId}) Tests
    // ==========================================

    // GET a single budget by ID - expects 200 with correct fields
    @Test
    void getBudget_existingBudget_returnsOk() throws Exception {
        when(budgetService.getBudget("w1", "acc1", "b1")).thenReturn(budget1);

        mockMvc.perform(get("/api/wallets/w1/accounts/acc1/budgets/b1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("b1")))
                .andExpect(jsonPath("$.categoryId", is("cat1")))
                .andExpect(jsonPath("$.limitAmount", is(100.00)));

        verify(budgetService, times(1)).getBudget("w1", "acc1", "b1");
    }

    // Budget ID doesn't exist - expects 404
    @Test
    void getBudget_nonExistentBudget_returnsNotFound() throws Exception {
        when(budgetService.getBudget("w1", "acc1", "unknown"))
                .thenThrow(new EntityNotFoundException("Budget: unknown"));

        mockMvc.perform(get("/api/wallets/w1/accounts/acc1/budgets/unknown")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // GET single budget with unknown wallet - expects 404
    @Test
    void getBudget_invalidWallet_returnsNotFound() throws Exception {
        when(budgetService.getBudget("unknown", "acc1", "b1"))
                .thenThrow(new EntityNotFoundException("Wallet: unknown"));

        mockMvc.perform(get("/api/wallets/unknown/accounts/acc1/budgets/b1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // GET single budget with unknown account - expects 404
    @Test
    void getBudget_invalidAccount_returnsNotFound() throws Exception {
        when(budgetService.getBudget("w1", "unknown", "b1"))
                .thenThrow(new EntityNotFoundException("Account: unknown"));

        mockMvc.perform(get("/api/wallets/w1/accounts/unknown/budgets/b1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // ==========================================
    // updateBudget (PATCH) Tests
    // ==========================================

    // Valid PATCH to update budget limit to 150 - expects 200 with updated value
    @Test
    void updateBudget_validRequest_returnsOk() throws Exception {
        BudgetResponse updatedBudget = new BudgetResponse(
                "b1",
                "user1",
                "w1",
                "acc1",
                "cat1",
                BigDecimal.valueOf(150.00),
                3,
                2026
        );
        when(budgetService.updateBudget(
                eq("w1"),
                eq("acc1"),
                eq("b1"),
                any(UpdateBudgetRequest.class)
        )).thenReturn(updatedBudget);

        mockMvc.perform(patch("/api/wallets/w1/accounts/acc1/budgets/b1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateBudgetRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("b1")))
                .andExpect(jsonPath("$.limitAmount", is(150.00)));

        verify(budgetService, times(1)).updateBudget(eq("w1"), eq("acc1"), eq("b1"), any(UpdateBudgetRequest.class));
    }

    // PATCH on non-existent budget - expects 404
    @Test
    void updateBudget_nonExistentBudget_returnsNotFound() throws Exception {
        when(budgetService.updateBudget(
                eq("w1"),
                eq("acc1"),
                eq("unknown"),
                any(UpdateBudgetRequest.class)
        )).thenThrow(new EntityNotFoundException("Budget: unknown"));

        mockMvc.perform(patch("/api/wallets/w1/accounts/acc1/budgets/unknown")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateBudgetRequest)))
                .andExpect(status().isNotFound());
    }

    // PATCH with unknown wallet - expects 404
    @Test
    void updateBudget_invalidWallet_returnsNotFound() throws Exception {
        when(budgetService.updateBudget(
                eq("unknown"),
                eq("acc1"),
                eq("b1"),
                any(UpdateBudgetRequest.class)
        )).thenThrow(new EntityNotFoundException("Wallet: unknown"));

        mockMvc.perform(patch("/api/wallets/unknown/accounts/acc1/budgets/b1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateBudgetRequest)))
                .andExpect(status().isNotFound());
    }

    // PATCH with unknown account - expects 404
    @Test
    void updateBudget_invalidAccount_returnsNotFound() throws Exception {
        when(budgetService.updateBudget(
                eq("w1"),
                eq("unknown"),
                eq("b1"),
                any(UpdateBudgetRequest.class)
        )).thenThrow(new EntityNotFoundException("Account: unknown"));

        mockMvc.perform(patch("/api/wallets/w1/accounts/unknown/budgets/b1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateBudgetRequest)))
                .andExpect(status().isNotFound());
    }

    // PATCH with empty body (no amountLimit) - expects 400 Bad Request
    @Test
    void updateBudget_nullAmountLimit_returnsBadRequest() throws Exception {
        String invalidRequest = "{}";

        mockMvc.perform(patch("/api/wallets/w1/accounts/acc1/budgets/b1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    // PATCH with negative amountLimit - expects 400 Bad Request
    @Test
    void updateBudget_negativeAmountLimit_returnsBadRequest() throws Exception {
        String invalidRequest = "{\"amountLimit\": -50}";

        mockMvc.perform(patch("/api/wallets/w1/accounts/acc1/budgets/b1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    // ==========================================
    // deleteBudget (DELETE) Tests
    // ==========================================

    // Valid DELETE of an existing budget - expects 204 No Content
    @Test
    void deleteBudget_existingBudget_returnsNoContent() throws Exception {
        doNothing().when(budgetService).deleteBudget("w1", "acc1", "b1");

        mockMvc.perform(delete("/api/wallets/w1/accounts/acc1/budgets/b1"))
                .andExpect(status().isNoContent());

        verify(budgetService, times(1)).deleteBudget("w1", "acc1", "b1");
    }

    // DELETE on non-existent budget - expects 404
    @Test
    void deleteBudget_nonExistentBudget_returnsNotFound() throws Exception {
        doThrow(new EntityNotFoundException("Budget: unknown")).when(budgetService).deleteBudget("w1", "acc1", "unknown");

        mockMvc.perform(delete("/api/wallets/w1/accounts/acc1/budgets/unknown"))
                .andExpect(status().isNotFound());
    }

    // DELETE with unknown wallet - expects 404
    @Test
    void deleteBudget_invalidWallet_returnsNotFound() throws Exception {
        doThrow(new EntityNotFoundException("Wallet: unknown")).when(budgetService).deleteBudget("unknown", "acc1", "b1");

        mockMvc.perform(delete("/api/wallets/unknown/accounts/acc1/budgets/b1"))
                .andExpect(status().isNotFound());
    }

    // DELETE with unknown account - expects 404
    @Test
    void deleteBudget_invalidAccount_returnsNotFound() throws Exception {
        doThrow(new EntityNotFoundException("Account: unknown")).when(budgetService).deleteBudget("w1", "unknown", "b1");

        mockMvc.perform(delete("/api/wallets/w1/accounts/unknown/budgets/b1"))
                .andExpect(status().isNotFound());
    }
}
    


