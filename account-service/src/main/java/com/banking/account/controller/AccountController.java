package com.banking.account.controller;

import com.banking.account.dto.AccountCreateRequest;
import com.banking.account.dto.AccountResponseDto;
import com.banking.account.dto.AccountStatusUpdateRequest;
import com.banking.account.service.AccountService;
import com.banking.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Bank Accounts", description = "Account creation, balance lookup, and lifecycle status management")
@SecurityRequirement(name = "BearerAuth")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @Operation(summary = "Open a bank account", description = "Creates a new savings or checking account with initial deposit")
    public ResponseEntity<ApiResponse<AccountResponseDto>> createAccount(@Valid @RequestBody AccountCreateRequest request) {
        AccountResponseDto created = accountService.createAccount(request);
        return new ResponseEntity<>(ApiResponse.success("Account opened successfully", created), HttpStatus.CREATED);
    }

    @GetMapping("/{accountNumber}")
    @Operation(summary = "Get account details", description = "Retrieves account metadata and current balance by account number")
    public ResponseEntity<ApiResponse<AccountResponseDto>> getAccountByNumber(@PathVariable String accountNumber) {
        AccountResponseDto account = accountService.getAccountByNumber(accountNumber);
        return ResponseEntity.ok(ApiResponse.success(account));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get accounts by customer ID", description = "Lists all bank accounts belonging to the specified customer")
    public ResponseEntity<ApiResponse<List<AccountResponseDto>>> getAccountsByCustomerId(@PathVariable Long customerId) {
        List<AccountResponseDto> accounts = accountService.getAccountsByCustomerId(customerId);
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }

    @PatchMapping("/{accountNumber}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT')")
    @Operation(summary = "Update account status", description = "Changes status to ACTIVE, BLOCKED, or CLOSED (Admin/Support only)")
    public ResponseEntity<ApiResponse<AccountResponseDto>> updateAccountStatus(
            @PathVariable String accountNumber,
            @Valid @RequestBody AccountStatusUpdateRequest request) {
        AccountResponseDto updated = accountService.updateAccountStatus(accountNumber, request);
        return ResponseEntity.ok(ApiResponse.success("Account status updated successfully", updated));
    }
}
