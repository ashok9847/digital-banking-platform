package com.banking.account.controller;

import com.banking.account.dto.BalanceUpdateRequest;
import com.banking.account.dto.BalanceUpdateResponse;
import com.banking.account.service.AccountService;
import com.banking.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts/internal")
@Tag(name = "Internal Account Operations", description = "Internal service-to-service endpoints for atomic balance debit/credit")
public class InternalAccountController {

    private final AccountService accountService;

    public InternalAccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/debit")
    @Operation(summary = "Internal debit", description = "Atomically debits the account with pessimistic write lock")
    public ResponseEntity<ApiResponse<BalanceUpdateResponse>> debit(@Valid @RequestBody BalanceUpdateRequest request) {
        BalanceUpdateResponse response = accountService.debitAccount(request);
        return ResponseEntity.ok(ApiResponse.success("Account debited successfully", response));
    }

    @PostMapping("/credit")
    @Operation(summary = "Internal credit", description = "Atomically credits the account with pessimistic write lock")
    public ResponseEntity<ApiResponse<BalanceUpdateResponse>> credit(@Valid @RequestBody BalanceUpdateRequest request) {
        BalanceUpdateResponse response = accountService.creditAccount(request);
        return ResponseEntity.ok(ApiResponse.success("Account credited successfully", response));
    }
}
