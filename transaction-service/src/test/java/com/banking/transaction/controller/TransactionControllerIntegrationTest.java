package com.banking.transaction.controller;

import com.banking.common.security.JwtTokenParser;
import com.banking.transaction.client.AccountClient;
import com.banking.transaction.client.BalanceResultDto;
import com.banking.transaction.dto.DepositRequest;
import com.banking.transaction.dto.TransferRequest;
import com.banking.transaction.repository.IdempotencyRecordRepository;
import com.banking.transaction.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @MockBean
    private JwtTokenParser jwtTokenParser;

    @MockBean
    private AccountClient accountClient;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        idempotencyRecordRepository.deleteAll();

        when(jwtTokenParser.validateToken(anyString())).thenReturn(true);
        when(jwtTokenParser.getUserId(anyString())).thenReturn(100L);
        when(jwtTokenParser.getUsername(anyString())).thenReturn("tx_user");
        when(jwtTokenParser.getEmail(anyString())).thenReturn("tx@banking.com");
        when(jwtTokenParser.getRoles(anyString())).thenReturn(List.of("ROLE_CUSTOMER"));
    }

    @Test
    @DisplayName("Should process deposit successfully via REST endpoint with Idempotency-Key")
    void deposit_Endpoint_Success() throws Exception {
        DepositRequest request = new DepositRequest("ACC1000", new BigDecimal("400.00"), "USD", "ATM Deposit");
        when(accountClient.credit(anyString(), any(), any())).thenReturn(
                new BalanceResultDto("ACC1000", new BigDecimal("1000.00"), new BigDecimal("1400.00"), "REF", "SUCCESS")
        );

        mockMvc.perform(post("/api/transactions/deposit")
                        .header("Authorization", "Bearer mock.jwt.token")
                        .header("Idempotency-Key", "idemp-test-deposit-101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.amount").value(400.00))
                .andExpect(jsonPath("$.data.destinationAccountNumber").value("ACC1000"));
    }

    @Test
    @DisplayName("Should process transfer successfully via REST endpoint")
    void transfer_Endpoint_Success() throws Exception {
        TransferRequest request = new TransferRequest("ACC1000", "ACC2000", new BigDecimal("150.00"), "USD", "Split bill");
        when(accountClient.debit(anyString(), any(), any())).thenReturn(
                new BalanceResultDto("ACC1000", new BigDecimal("1400.00"), new BigDecimal("1250.00"), "REF", "SUCCESS")
        );
        when(accountClient.credit(anyString(), any(), any())).thenReturn(
                new BalanceResultDto("ACC2000", new BigDecimal("500.00"), new BigDecimal("650.00"), "REF", "SUCCESS")
        );

        mockMvc.perform(post("/api/transactions/transfer")
                        .header("Authorization", "Bearer mock.jwt.token")
                        .header("Idempotency-Key", "idemp-test-transfer-202")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.amount").value(150.00))
                .andExpect(jsonPath("$.data.sourceAccountNumber").value("ACC1000"))
                .andExpect(jsonPath("$.data.destinationAccountNumber").value("ACC2000"));
    }

    @Test
    @DisplayName("Idempotency: Repeated deposit with same key returns cached response and never credits twice")
    void deposit_RepeatedRequest_ReturnsCached() throws Exception {
        DepositRequest request = new DepositRequest("ACC1000", new BigDecimal("300.00"), "USD", "Repeated test");
        when(accountClient.credit(anyString(), any(), any())).thenReturn(
                new BalanceResultDto("ACC1000", new BigDecimal("1000.00"), new BigDecimal("1300.00"), "REF", "SUCCESS")
        );

        String key = "idemp-repeat-dep-001";

        // 1. First request
        String firstResponse = mockMvc.perform(post("/api/transactions/deposit")
                        .header("Authorization", "Bearer mock.jwt.token")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // 2. Repeated request with same key and payload
        mockMvc.perform(post("/api/transactions/deposit")
                        .header("Authorization", "Bearer mock.jwt.token")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.amount").value(300.00));

        // Verify credit was called exactly ONCE
        org.mockito.Mockito.verify(accountClient, org.mockito.Mockito.times(1))
                .credit(anyString(), any(), any());
    }

    @Test
    @DisplayName("Idempotency: Reusing key with different payload returns 409 Conflict")
    void deposit_SameKeyDifferentPayload_Returns409Conflict() throws Exception {
        DepositRequest request1 = new DepositRequest("ACC1000", new BigDecimal("100.00"), "USD", "Initial");
        DepositRequest request2 = new DepositRequest("ACC1000", new BigDecimal("200.00"), "USD", "Modified");
        when(accountClient.credit(anyString(), any(), any())).thenReturn(
                new BalanceResultDto("ACC1000", new BigDecimal("1000.00"), new BigDecimal("1100.00"), "REF", "SUCCESS")
        );

        String key = "idemp-conflict-key-002";

        // First request succeeds
        mockMvc.perform(post("/api/transactions/deposit")
                        .header("Authorization", "Bearer mock.jwt.token")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // Second request with same key but different payload fails with 409 Conflict
        mockMvc.perform(post("/api/transactions/deposit")
                        .header("Authorization", "Bearer mock.jwt.token")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DUPLICATE_RESOURCE"));
    }

    @Test
    @DisplayName("Idempotency: Withdraw repeated request returns cached response")
    void withdraw_RepeatedRequest_ReturnsCached() throws Exception {
        com.banking.transaction.dto.WithdrawRequest request =
                new com.banking.transaction.dto.WithdrawRequest("ACC1000", new BigDecimal("50.00"), "USD", "ATM");
        when(accountClient.debit(anyString(), any(), any())).thenReturn(
                new BalanceResultDto("ACC1000", new BigDecimal("1000.00"), new BigDecimal("950.00"), "REF", "SUCCESS")
        );

        String key = "idemp-wth-repeat-003";

        // First request
        mockMvc.perform(post("/api/transactions/withdraw")
                        .header("Authorization", "Bearer mock.jwt.token")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.amount").value(50.00));

        // Repeated request
        mockMvc.perform(post("/api/transactions/withdraw")
                        .header("Authorization", "Bearer mock.jwt.token")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.amount").value(50.00));

        org.mockito.Mockito.verify(accountClient, org.mockito.Mockito.times(1))
                .debit(anyString(), any(), any());
    }
}
