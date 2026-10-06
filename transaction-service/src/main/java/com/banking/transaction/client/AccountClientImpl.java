package com.banking.transaction.client;

import com.banking.common.dto.ApiResponse;
import com.banking.common.exception.BankingException;
import com.banking.common.exception.InsufficientBalanceException;
import com.banking.common.exception.InvalidTransactionException;
import com.banking.common.exception.ResourceNotFoundException;
import com.banking.common.filter.CorrelationIdFilter;
import com.banking.common.security.SecurityConstants;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class AccountClientImpl implements AccountClient {

    private final RestClient restClient;
    private final String internalServiceKey;

    public AccountClientImpl(
            @Value("${services.account-service.url:http://localhost:8083}") String accountServiceUrl,
            @Value("${internal.service.key:${INTERNAL_SERVICE_KEY:}}") String internalServiceKey) {
        this.internalServiceKey = internalServiceKey;
        this.restClient = RestClient.builder()
                .baseUrl(accountServiceUrl)
                .build();
    }

    @Override
    public AccountDto getAccount(String accountNumber, String bearerToken) {
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);

        ApiResponse<AccountDto> response = restClient.get()
                .uri("/api/accounts/{accountNumber}", accountNumber)
                .header(SecurityConstants.AUTH_HEADER, bearerToken != null ? bearerToken : "")
                .header(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId != null ? correlationId : "")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                    if (resp.getStatusCode().value() == 404) {
                        throw new ResourceNotFoundException("Account", "accountNumber", accountNumber);
                    }
                    throw new BankingException("Account service client error: " + resp.getStatusCode());
                })
                .body(new ParameterizedTypeReference<ApiResponse<AccountDto>>() {});

        if (response != null && response.getData() != null) {
            return response.getData();
        }
        throw new ResourceNotFoundException("Account", "accountNumber", accountNumber);
    }

    @Override
    public BalanceResultDto debit(String accountNumber, BigDecimal amount, String reference) {
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);
        Map<String, Object> body = Map.of(
                "accountNumber", accountNumber,
                "amount", amount,
                "transactionReference", reference
        );

        ApiResponse<BalanceResultDto> response = restClient.post()
                .uri("/api/accounts/internal/debit")
                .header(SecurityConstants.INTERNAL_SERVICE_HEADER, internalServiceKey)
                .header(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId != null ? correlationId : "")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                    if (resp.getStatusCode().value() == 404) {
                        throw new ResourceNotFoundException("Account", "accountNumber", accountNumber);
                    }
                    if (resp.getStatusCode().value() == 400) {
                        throw new InsufficientBalanceException("Insufficient balance or inactive account " + accountNumber);
                    }
                    throw new InvalidTransactionException("Failed to debit account " + accountNumber);
                })
                .body(new ParameterizedTypeReference<ApiResponse<BalanceResultDto>>() {});

        if (response != null && response.getData() != null) {
            return response.getData();
        }
        throw new InvalidTransactionException("Empty response from account debit");
    }

    @Override
    public BalanceResultDto credit(String accountNumber, BigDecimal amount, String reference) {
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);
        Map<String, Object> body = Map.of(
                "accountNumber", accountNumber,
                "amount", amount,
                "transactionReference", reference
        );

        ApiResponse<BalanceResultDto> response = restClient.post()
                .uri("/api/accounts/internal/credit")
                .header(SecurityConstants.INTERNAL_SERVICE_HEADER, internalServiceKey)
                .header(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId != null ? correlationId : "")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                    if (resp.getStatusCode().value() == 404) {
                        throw new ResourceNotFoundException("Account", "accountNumber", accountNumber);
                    }
                    throw new InvalidTransactionException("Failed to credit account " + accountNumber);
                })
                .body(new ParameterizedTypeReference<ApiResponse<BalanceResultDto>>() {});

        if (response != null && response.getData() != null) {
            return response.getData();
        }
        throw new InvalidTransactionException("Empty response from account credit");
    }
}
