package com.banking.account.dto;

import com.banking.account.entity.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public class AccountCreateRequest {

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotNull(message = "Account type is required (SAVINGS or CURRENT)")
    private AccountType accountType;

    @DecimalMin(value = "0.00", inclusive = true, message = "Initial deposit cannot be negative")
    private BigDecimal initialDeposit = BigDecimal.ZERO;

    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code (e.g. USD, EUR)")
    private String currency = "USD";

    public AccountCreateRequest() {}

    public AccountCreateRequest(Long customerId, AccountType accountType, BigDecimal initialDeposit, String currency) {
        this.customerId = customerId;
        this.accountType = accountType;
        this.initialDeposit = initialDeposit != null ? initialDeposit : BigDecimal.ZERO;
        this.currency = currency != null ? currency : "USD";
    }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public AccountType getAccountType() { return accountType; }
    public void setAccountType(AccountType accountType) { this.accountType = accountType; }

    public BigDecimal getInitialDeposit() { return initialDeposit; }
    public void setInitialDeposit(BigDecimal initialDeposit) { this.initialDeposit = initialDeposit; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
