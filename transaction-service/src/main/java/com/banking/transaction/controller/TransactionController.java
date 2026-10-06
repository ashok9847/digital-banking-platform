package com.banking.transaction.controller;

import com.banking.common.dto.ApiResponse;
import com.banking.common.security.SecurityConstants;
import com.banking.transaction.dto.DepositRequest;
import com.banking.transaction.dto.TransactionResponseDto;
import com.banking.transaction.dto.TransferRequest;
import com.banking.transaction.dto.WithdrawRequest;
import com.banking.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Financial Transactions", description = "Deposit, withdrawal, account transfer, and ledger history endpoints")
@SecurityRequirement(name = "BearerAuth")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/deposit")
    @Operation(summary = "Deposit funds", description = "Deposits money into an active bank account with Idempotency-Key support")
    public ResponseEntity<ApiResponse<TransactionResponseDto>> deposit(
            @RequestHeader(value = SecurityConstants.IDEMPOTENCY_KEY_HEADER, required = false)
            @Parameter(description = "Unique client-generated idempotency key (UUID)") String idempotencyKey,
            @Valid @RequestBody DepositRequest request) {

        String key = resolveIdempotencyKey(idempotencyKey);
        TransactionResponseDto result = transactionService.deposit(request, key);
        return new ResponseEntity<>(ApiResponse.success("Deposit processed successfully", result), HttpStatus.CREATED);
    }

    @PostMapping("/withdraw")
    @Operation(summary = "Withdraw funds", description = "Withdraws money from an active account with balance verification and Idempotency-Key support")
    public ResponseEntity<ApiResponse<TransactionResponseDto>> withdraw(
            @RequestHeader(value = SecurityConstants.IDEMPOTENCY_KEY_HEADER, required = false)
            @Parameter(description = "Unique client-generated idempotency key (UUID)") String idempotencyKey,
            @Valid @RequestBody WithdrawRequest request) {

        String key = resolveIdempotencyKey(idempotencyKey);
        TransactionResponseDto result = transactionService.withdraw(request, key);
        return new ResponseEntity<>(ApiResponse.success("Withdrawal processed successfully", result), HttpStatus.CREATED);
    }

    @PostMapping("/transfer")
    @Operation(summary = "Transfer money", description = "Executes an atomic transfer between two accounts with balance check and idempotency protection")
    public ResponseEntity<ApiResponse<TransactionResponseDto>> transfer(
            @RequestHeader(value = SecurityConstants.IDEMPOTENCY_KEY_HEADER, required = false)
            @Parameter(description = "Unique client-generated idempotency key (UUID)") String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {

        String key = resolveIdempotencyKey(idempotencyKey);
        TransactionResponseDto result = transactionService.transfer(request, key);
        return new ResponseEntity<>(ApiResponse.success("Transfer completed successfully", result), HttpStatus.CREATED);
    }

    @GetMapping("/{transactionReference}")
    @Operation(summary = "Get transaction details", description = "Fetches a transaction journal entry by its reference code")
    public ResponseEntity<ApiResponse<TransactionResponseDto>> getTransactionByReference(
            @PathVariable String transactionReference) {
        TransactionResponseDto tx = transactionService.getTransactionByReference(transactionReference);
        return ResponseEntity.ok(ApiResponse.success(tx));
    }

    @GetMapping("/account/{accountNumber}")
    @Operation(summary = "Get account transaction history", description = "Returns paginated debit and credit transaction history for an account")
    public ResponseEntity<ApiResponse<Page<TransactionResponseDto>>> getTransactionsByAccount(
            @PathVariable String accountNumber,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<TransactionResponseDto> page = transactionService.getTransactionsByAccount(accountNumber, pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    private String resolveIdempotencyKey(String headerKey) {
        return (headerKey != null && !headerKey.trim().isEmpty()) ? headerKey.trim() : UUID.randomUUID().toString();
    }
}
