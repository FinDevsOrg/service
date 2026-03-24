package ro.unibuc.prodeng.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ro.unibuc.prodeng.model.TransactionType;
import ro.unibuc.prodeng.response.TransactionResponse;
import ro.unibuc.prodeng.service.TransactionService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
class TransactionControllerTest {

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private TransactionController transactionController;

    private MockMvc mockMvc;

    private final LocalDateTime NOW = LocalDateTime.of(2024, 1, 1, 12, 0);

    private TransactionResponse tx1 = new TransactionResponse("tx1", "w1", "acc1", "cat1", BigDecimal.TEN, TransactionType.INCOME, NOW, "desc1");
    private TransactionResponse tx2 = new TransactionResponse("tx2", "w2", "acc2", "cat2", BigDecimal.valueOf(20), TransactionType.EXPENSE, NOW, "desc2");

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(transactionController).build();
    }

    @Test
    void getTransactions_noFilters_returnsAllTransactions() throws Exception {
        when(transactionService.filterTransactions(null, null, null, null)).thenReturn(List.of(tx1, tx2));

        mockMvc.perform(get("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is("tx1")))
                .andExpect(jsonPath("$[1].id", is("tx2")));

        verify(transactionService, times(1)).filterTransactions(null, null, null, null);
    }

    @Test
    void getTransactions_withUserId_returnsFilteredTransactions() throws Exception {
        when(transactionService.filterTransactions(null, null, null, "user1")).thenReturn(List.of(tx1));

        mockMvc.perform(get("/api/transactions")
                .param("userId", "user1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is("tx1")));

        verify(transactionService, times(1)).filterTransactions(null, null, null, "user1");
    }

    @Test
    void getTransactions_withWalletId_returnsFilteredTransactions() throws Exception {
        when(transactionService.filterTransactions("w2", null, null, null)).thenReturn(List.of(tx2));

        mockMvc.perform(get("/api/transactions")
                .param("walletId", "w2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is("tx2")));

        verify(transactionService, times(1)).filterTransactions("w2", null, null, null);
    }

    @Test
    void getTransactions_emptyResult_returnsEmptyList() throws Exception {
        when(transactionService.filterTransactions(null, null, null, "unknown")).thenReturn(List.of());

        mockMvc.perform(get("/api/transactions")
                .param("userId", "unknown")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
