package com.banking.account.controller;

import com.banking.account.dto.AccountCreateRequest;
import com.banking.account.dto.BalanceUpdateRequest;
import com.banking.account.entity.AccountType;
import com.banking.account.repository.AccountRepository;
import com.banking.common.security.JwtTokenParser;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private com.banking.account.repository.AccountLimitRepository accountLimitRepository;

    @MockBean
    private JwtTokenParser jwtTokenParser;

    @org.springframework.beans.factory.annotation.Value("${internal.service.key}")
    private String internalServiceKey;

    @BeforeEach
    void setUp() {
        accountLimitRepository.deleteAll();
        accountRepository.deleteAll();

        when(jwtTokenParser.validateToken(anyString())).thenReturn(true);
        when(jwtTokenParser.getUserId(anyString())).thenReturn(77L);
        when(jwtTokenParser.getUsername(anyString())).thenReturn("bank_customer");
        when(jwtTokenParser.getEmail(anyString())).thenReturn("customer@bank.com");
        when(jwtTokenParser.getRoles(anyString())).thenReturn(List.of("ROLE_CUSTOMER"));
    }

    @Test
    @DisplayName("Complete Account Lifecycle: Create -> Get -> Internal Debit -> Internal Credit")
    void accountLifecycleIntegrationTest() throws Exception {
        AccountCreateRequest createReq = new AccountCreateRequest(
                77L, AccountType.SAVINGS, new BigDecimal("1500.0000"), "USD"
        );

        // 1. Create account
        String responseStr = mockMvc.perform(post("/api/accounts")
                        .header("Authorization", "Bearer valid.mock.jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accountNumber").isString())
                .andExpect(jsonPath("$.data.balance").value(1500.0000))
                .andReturn().getResponse().getContentAsString();

        String accountNumber = objectMapper.readTree(responseStr).get("data").get("accountNumber").asText();

        // 2. Query account
        mockMvc.perform(get("/api/accounts/" + accountNumber)
                        .header("Authorization", "Bearer valid.mock.jwt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountNumber").value(accountNumber))
                .andExpect(jsonPath("$.data.balance").value(1500.0000));

        // 3. Internal Debit with valid service key
        BalanceUpdateRequest debitReq = new BalanceUpdateRequest(accountNumber, new BigDecimal("500.0000"), "TXN-001");
        mockMvc.perform(post("/api/accounts/internal/debit")
                        .header("X-Internal-Service-Key", internalServiceKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(debitReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.previousBalance").value(1500.0000))
                .andExpect(jsonPath("$.data.newBalance").value(1000.0000));

        // 4. Internal Credit with valid service key
        BalanceUpdateRequest creditReq = new BalanceUpdateRequest(accountNumber, new BigDecimal("200.0000"), "TXN-002");
        mockMvc.perform(post("/api/accounts/internal/credit")
                        .header("X-Internal-Service-Key", internalServiceKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creditReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.previousBalance").value(1000.0000))
                .andExpect(jsonPath("$.data.newBalance").value(1200.0000));
    }

    @Test
    @DisplayName("Internal API Security: Anonymous request without internal key is rejected (401)")
    void internalDebit_Anonymous_ReturnsUnauthorized() throws Exception {
        BalanceUpdateRequest debitReq = new BalanceUpdateRequest("ACC100200", new BigDecimal("100.0000"), "TXN-ANON");

        mockMvc.perform(post("/api/accounts/internal/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(debitReq)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Internal API Security: Invalid internal service key is rejected (401)")
    void internalDebit_InvalidServiceKey_ReturnsUnauthorized() throws Exception {
        BalanceUpdateRequest debitReq = new BalanceUpdateRequest("ACC100200", new BigDecimal("100.0000"), "TXN-BADKEY");

        mockMvc.perform(post("/api/accounts/internal/debit")
                        .header("X-Internal-Service-Key", "wrong-secret-key-999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(debitReq)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Internal API Security: Customer JWT without internal key is rejected (403)")
    void internalDebit_CustomerJwtWithoutInternalKey_ReturnsForbidden() throws Exception {
        BalanceUpdateRequest debitReq = new BalanceUpdateRequest("ACC100200", new BigDecimal("100.0000"), "TXN-CUST");

        mockMvc.perform(post("/api/accounts/internal/debit")
                        .header("Authorization", "Bearer valid.mock.jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(debitReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Internal API Security: Empty or malformed internal key is rejected (401)")
    void internalDebit_EmptyOrMalformedKey_ReturnsUnauthorized() throws Exception {
        BalanceUpdateRequest debitReq = new BalanceUpdateRequest("ACC100200", new BigDecimal("100.0000"), "TXN-MALFORMED");

        // Empty string
        mockMvc.perform(post("/api/accounts/internal/debit")
                        .header("X-Internal-Service-Key", "")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(debitReq)))
                .andExpect(status().isUnauthorized());

        // Whitespace only
        mockMvc.perform(post("/api/accounts/internal/debit")
                        .header("X-Internal-Service-Key", "   ")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(debitReq)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Internal API Security: Valid internal service key is accepted (200)")
    void internalCredit_ValidServiceKey_ReturnsOk() throws Exception {
        // Pre-seed an account
        AccountCreateRequest createReq = new AccountCreateRequest(
                77L, AccountType.CURRENT, new BigDecimal("500.0000"), "USD"
        );

        String res = mockMvc.perform(post("/api/accounts")
                        .header("Authorization", "Bearer valid.mock.jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String accNo = objectMapper.readTree(res).get("data").get("accountNumber").asText();

        BalanceUpdateRequest creditReq = new BalanceUpdateRequest(accNo, new BigDecimal("300.0000"), "TXN-VALID-SRV");

        mockMvc.perform(post("/api/accounts/internal/credit")
                        .header("X-Internal-Service-Key", internalServiceKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creditReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.previousBalance").value(500.0000))
                .andExpect(jsonPath("$.data.newBalance").value(800.0000));
    }
}
