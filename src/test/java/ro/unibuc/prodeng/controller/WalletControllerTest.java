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
import ro.unibuc.prodeng.exception.EntityNotFoundException;
import ro.unibuc.prodeng.exception.GlobalExceptionHandler;
import ro.unibuc.prodeng.model.TransactionType;
import ro.unibuc.prodeng.request.CreateAccountRequest;
import ro.unibuc.prodeng.request.TransactionRequest;
import ro.unibuc.prodeng.request.UpdateAccountBalanceRequest;
import ro.unibuc.prodeng.response.AccountResponse;
import ro.unibuc.prodeng.response.TransactionResponse;
import ro.unibuc.prodeng.response.WalletResponse;
import ro.unibuc.prodeng.service.WalletService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@ExtendWith(SpringExtension.class)
class WalletControllerTest {

    @Mock
    private WalletService walletService;

    @InjectMocks
    private WalletController walletController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final AccountResponse account1 = new AccountResponse("acc1", "CHECKING", "RON", BigDecimal.valueOf(450.50));
    private final AccountResponse account2 = new AccountResponse("acc2", "SAVINGS", "EUR", BigDecimal.valueOf(100.00));
    private final WalletResponse walletResponse = new WalletResponse("w1", "user1", List.of(account1, account2));

    private final CreateAccountRequest createAccountRequest = new CreateAccountRequest(
            "CHECKING",
            "RON",
            BigDecimal.valueOf(250.00)
    );

    private final TransactionRequest transactionRequest = new TransactionRequest(
            TransactionType.EXPENSE,
            BigDecimal.valueOf(70.00),
            "cat1",
            "Groceries"
    );

    private final UpdateAccountBalanceRequest updateBalanceRequest = new UpdateAccountBalanceRequest(
            BigDecimal.valueOf(50.00)
    );

    private final TransactionResponse tx1 = new TransactionResponse(
            "tx1",
            "w1",
            "acc1",
            "cat1",
            BigDecimal.valueOf(70.00),
            TransactionType.EXPENSE,
            LocalDateTime.of(2026, 3, 1, 10, 0),
            "Groceries"
    );

    private final TransactionResponse tx2 = new TransactionResponse(
            "tx2",
            "w1",
            "acc1",
            "cat2",
            BigDecimal.valueOf(250.00),
            TransactionType.INCOME,
            LocalDateTime.of(2026, 3, 2, 9, 30),
            "Salary"
    );

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(walletController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // POST valid user ID - expects 201 Created with wallet and 2 accounts
    @Test
    void createWallet_validUserId_returnsCreated() throws Exception {
        when(walletService.createWallet("user1")).thenReturn(walletResponse);

        mockMvc.perform(post("/api/wallets/user/user1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is("w1")))
                .andExpect(jsonPath("$.userId", is("user1")))
                .andExpect(jsonPath("$.accounts", hasSize(2)));

        verify(walletService, times(1)).createWallet("user1");
    }

    // POST with unknown user - expects 404
    @Test
    void createWallet_userNotFound_returnsNotFound() throws Exception {
        when(walletService.createWallet("unknown"))
                .thenThrow(new EntityNotFoundException("User not found"));

        mockMvc.perform(post("/api/wallets/user/unknown"))
                .andExpect(status().isNotFound());
    }

    // GET wallet by existing user ID - expects 200 with wallet data
    @Test
    void getWalletByUserId_existingWallet_returnsOk() throws Exception {
        when(walletService.getWalletByUserId("user1")).thenReturn(walletResponse);

        mockMvc.perform(get("/api/wallets/user/user1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("w1")))
                .andExpect(jsonPath("$.accounts", hasSize(2)));

        verify(walletService, times(1)).getWalletByUserId("user1");
    }

    // GET wallet by unknown user ID - expects 404
    @Test
    void getWalletByUserId_notFound_returnsNotFound() throws Exception {
        when(walletService.getWalletByUserId("unknown"))
                .thenThrow(new EntityNotFoundException("Wallet not found"));

        mockMvc.perform(get("/api/wallets/user/unknown"))
                .andExpect(status().isNotFound());
    }

    // GET wallet by wallet ID - expects 200 with correct id and userId
    @Test
    void getWalletById_existingWallet_returnsOk() throws Exception {
        when(walletService.getWalletById("w1")).thenReturn(walletResponse);

        mockMvc.perform(get("/api/wallets/w1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("w1")))
                .andExpect(jsonPath("$.userId", is("user1")));

        verify(walletService, times(1)).getWalletById("w1");
    }

    // GET wallet by missing wallet ID - expects 404
    @Test
    void getWalletById_notFound_returnsNotFound() throws Exception {
        when(walletService.getWalletById("missing"))
                .thenThrow(new EntityNotFoundException("Wallet not found"));

        mockMvc.perform(get("/api/wallets/missing"))
                .andExpect(status().isNotFound());
    }

    // GET existing account - expects 200 with correct id, currency, and balance
    @Test
    void getAccount_existingAccount_returnsOk() throws Exception {
        when(walletService.getAccount("w1", "acc1")).thenReturn(account1);

        mockMvc.perform(get("/api/wallets/w1/accounts/acc1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("acc1")))
                .andExpect(jsonPath("$.currency", is("RON")))
                .andExpect(jsonPath("$.balance", is(450.50)));

        verify(walletService, times(1)).getAccount("w1", "acc1");
    }

    // GET missing account - expects 404
    @Test
    void getAccount_notFound_returnsNotFound() throws Exception {
        when(walletService.getAccount("w1", "missing"))
                .thenThrow(new EntityNotFoundException("Account not found"));

        mockMvc.perform(get("/api/wallets/w1/accounts/missing"))
                .andExpect(status().isNotFound());
    }

    // GET transactions for existing account - expects 200 with 2 transactions (EXPENSE + INCOME)
    @Test
    void getAccountTransactions_existingAccount_returnsTransactionList() throws Exception {
        when(walletService.getAccountTransactions("w1", "acc1")).thenReturn(List.of(tx1, tx2));

        mockMvc.perform(get("/api/wallets/w1/accounts/acc1/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is("tx1")))
                .andExpect(jsonPath("$[0].type", is("EXPENSE")))
                .andExpect(jsonPath("$[1].id", is("tx2")))
                .andExpect(jsonPath("$[1].type", is("INCOME")));

        verify(walletService, times(1)).getAccountTransactions("w1", "acc1");
    }

    // GET transactions for missing account - expects 404
    @Test
    void getAccountTransactions_notFound_returnsNotFound() throws Exception {
        when(walletService.getAccountTransactions("w1", "missing"))
                .thenThrow(new EntityNotFoundException("Account not found"));

        mockMvc.perform(get("/api/wallets/w1/accounts/missing/transactions"))
                .andExpect(status().isNotFound());
    }

    // POST valid account creation request - expects 201 with updated wallet
    @Test
    void addAccount_validRequest_returnsCreated() throws Exception {
        when(walletService.addAccount(eq("w1"), any(CreateAccountRequest.class))).thenReturn(walletResponse);

        mockMvc.perform(post("/api/wallets/w1/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createAccountRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is("w1")))
                .andExpect(jsonPath("$.accounts", hasSize(2)));

        verify(walletService, times(1)).addAccount(eq("w1"), any(CreateAccountRequest.class));
    }

    // POST with lowercase currency "ron" - fails validation, expects 400, service never called
    @Test
    void addAccount_invalidCurrency_returnsBadRequest() throws Exception {
        String invalidRequest = "{\"type\":\"CHECKING\",\"currency\":\"ron\",\"initialBalance\":100}";

        mockMvc.perform(post("/api/wallets/w1/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(walletService);
    }

    // POST account to missing wallet - expects 404
    @Test
    void addAccount_walletNotFound_returnsNotFound() throws Exception {
        when(walletService.addAccount(eq("missing"), any(CreateAccountRequest.class)))
                .thenThrow(new EntityNotFoundException("Wallet not found"));

        mockMvc.perform(post("/api/wallets/missing/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createAccountRequest)))
                .andExpect(status().isNotFound());
    }

    // POST valid transaction (expense) - expects 201
    @Test
    void createTransaction_validRequest_returnsCreated() throws Exception {
        when(walletService.createTransaction(eq("w1"), eq("acc1"), any(TransactionRequest.class)))
                .thenReturn(walletResponse);

        mockMvc.perform(post("/api/wallets/w1/accounts/acc1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transactionRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is("w1")));

        verify(walletService, times(1)).createTransaction(eq("w1"), eq("acc1"), any(TransactionRequest.class));
    }

    // POST transaction with negative amount - fails validation, expects 400, service never called
    @Test
    void createTransaction_negativeAmount_returnsBadRequest() throws Exception {
        String invalidRequest = "{\"type\":\"EXPENSE\",\"amount\":-5,\"categoryId\":\"cat1\",\"description\":\"bad\"}";

        mockMvc.perform(post("/api/wallets/w1/accounts/acc1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(walletService);
    }

    // POST transaction to missing account - expects 404
    @Test
    void createTransaction_notFound_returnsNotFound() throws Exception {
        when(walletService.createTransaction(eq("w1"), eq("missing"), any(TransactionRequest.class)))
                .thenThrow(new EntityNotFoundException("Account not found"));

        mockMvc.perform(post("/api/wallets/w1/accounts/missing/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transactionRequest)))
                .andExpect(status().isNotFound());
    }

    // DELETE existing wallet - expects 204 No Content
    @Test
    void deleteWallet_existingWallet_returnsNoContent() throws Exception {
        doNothing().when(walletService).deleteWallet("w1");

        mockMvc.perform(delete("/api/wallets/w1"))
                .andExpect(status().isNoContent());

        verify(walletService, times(1)).deleteWallet("w1");
    }

    // DELETE missing wallet - expects 404
    @Test
    void deleteWallet_notFound_returnsNotFound() throws Exception {
        doThrow(new EntityNotFoundException("Wallet not found")).when(walletService).deleteWallet("missing");

        mockMvc.perform(delete("/api/wallets/missing"))
                .andExpect(status().isNotFound());
    }

    // DELETE existing account - expects 204 No Content
    @Test
    void deleteAccount_existingAccount_returnsNoContent() throws Exception {
        doNothing().when(walletService).deleteAccount("w1", "acc1");

        mockMvc.perform(delete("/api/wallets/w1/accounts/acc1"))
                .andExpect(status().isNoContent());

        verify(walletService, times(1)).deleteAccount("w1", "acc1");
    }

    // DELETE missing account - expects 404
    @Test
    void deleteAccount_notFound_returnsNotFound() throws Exception {
        doThrow(new EntityNotFoundException("Account not found")).when(walletService).deleteAccount("w1", "missing");

        mockMvc.perform(delete("/api/wallets/w1/accounts/missing"))
                .andExpect(status().isNotFound());
    }


    // PATCH valid deposit of 50 - expects 200 with updated wallet
    @Test
    void deposit_validRequest_returnsOk() throws Exception {
        when(walletService.deposit(eq("w1"), eq("acc1"), any(UpdateAccountBalanceRequest.class)))
                .thenReturn(walletResponse);

        mockMvc.perform(patch("/api/wallets/w1/accounts/acc1/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateBalanceRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("w1")));

        verify(walletService, times(1)).deposit(eq("w1"), eq("acc1"), any(UpdateAccountBalanceRequest.class));
    }

    // PATCH deposit with zero amount - fails validation, expects 400, service never called
    @Test
    void deposit_zeroAmount_returnsBadRequest() throws Exception {
        String invalidRequest = "{\"amount\":0}";

        mockMvc.perform(patch("/api/wallets/w1/accounts/acc1/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(walletService);
    }

    // PATCH deposit to missing account - expects 404
    @Test
    void deposit_notFound_returnsNotFound() throws Exception {
        when(walletService.deposit(eq("w1"), eq("missing"), any(UpdateAccountBalanceRequest.class)))
                .thenThrow(new EntityNotFoundException("Account not found"));

        mockMvc.perform(patch("/api/wallets/w1/accounts/missing/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateBalanceRequest)))
                .andExpect(status().isNotFound());
    }

    // PATCH valid withdrawal of 50 - expects 200 with updated wallet
    @Test
    void withdraw_validRequest_returnsOk() throws Exception {
        when(walletService.withdraw(eq("w1"), eq("acc1"), any(UpdateAccountBalanceRequest.class)))
                .thenReturn(walletResponse);

        mockMvc.perform(patch("/api/wallets/w1/accounts/acc1/withdraw")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateBalanceRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("w1")));

        verify(walletService, times(1)).withdraw(eq("w1"), eq("acc1"), any(UpdateAccountBalanceRequest.class));
    }

    // PATCH withdrawal with zero amount - fails validation, expects 400, service never called
    @Test
    void withdraw_zeroAmount_returnsBadRequest() throws Exception {
        String invalidRequest = "{\"amount\":0}";

        mockMvc.perform(patch("/api/wallets/w1/accounts/acc1/withdraw")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(walletService);
    }

    // PATCH withdrawal from missing account - expects 404
    @Test
    void withdraw_notFound_returnsNotFound() throws Exception {
        when(walletService.withdraw(eq("w1"), eq("missing"), any(UpdateAccountBalanceRequest.class)))
                .thenThrow(new EntityNotFoundException("Account not found"));

        mockMvc.perform(patch("/api/wallets/w1/accounts/missing/withdraw")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateBalanceRequest)))
                .andExpect(status().isNotFound());
    }
}