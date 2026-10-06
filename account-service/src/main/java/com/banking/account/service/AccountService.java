package com.banking.account.service;

import com.banking.account.dto.*;
import com.banking.account.entity.*;
import com.banking.account.repository.AccountLimitRepository;
import com.banking.account.repository.AccountRepository;
import com.banking.common.exception.ForbiddenException;
import com.banking.common.exception.InsufficientBalanceException;
import com.banking.common.exception.InvalidTransactionException;
import com.banking.common.exception.ResourceNotFoundException;
import com.banking.common.security.AuthenticatedUser;
import com.banking.common.security.SecurityHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);
    private final AccountRepository accountRepository;
    private final AccountLimitRepository accountLimitRepository;
    private final SecureRandom random = new SecureRandom();

    public AccountService(AccountRepository accountRepository, AccountLimitRepository accountLimitRepository) {
        this.accountRepository = accountRepository;
        this.accountLimitRepository = accountLimitRepository;
    }

    @Transactional
    public AccountResponseDto createAccount(AccountCreateRequest request) {
        String accountNumber = generateAccountNumber();

        Account account = new Account(
                accountNumber,
                request.getCustomerId(),
                request.getAccountType(),
                request.getInitialDeposit(),
                request.getCurrency()
        );

        Account saved = accountRepository.save(account);

        AccountLimit limit = new AccountLimit(
                saved,
                new BigDecimal("50000.0000"),
                new BigDecimal("10000.0000")
        );
        accountLimitRepository.save(limit);

        log.info("Created account {} for customerId {}", accountNumber, request.getCustomerId());
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public AccountResponseDto getAccountByNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountNumber", accountNumber));

        validateOwnership(account.getCustomerId());
        return mapToDto(account);
    }

    @Transactional(readOnly = true)
    public List<AccountResponseDto> getAccountsByCustomerId(Long customerId) {
        validateOwnership(customerId);
        return accountRepository.findByCustomerId(customerId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public AccountResponseDto updateAccountStatus(String accountNumber, AccountStatusUpdateRequest request) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountNumber", accountNumber));

        account.setStatus(request.getStatus());
        Account updated = accountRepository.save(account);
        log.info("Account {} status updated to {}", accountNumber, request.getStatus());
        return mapToDto(updated);
    }

    /**
     * Atomically debits the account using database-level pessimistic write locking (SELECT ... FOR UPDATE).
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BalanceUpdateResponse debitAccount(BalanceUpdateRequest request) {
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Debit amount must be strictly greater than 0");
        }

        Account account = accountRepository.findWithPessimisticLockByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountNumber", request.getAccountNumber()));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidTransactionException("Cannot debit inactive account " + request.getAccountNumber() +
                    " (Status: " + account.getStatus() + ")");
        }

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            log.warn("Debit rejected: insufficient funds in account {}. Available: {}, Requested: {}",
                    account.getAccountNumber(), account.getBalance(), request.getAmount());
            throw new InsufficientBalanceException(account.getAccountNumber(), account.getBalance(), request.getAmount());
        }

        BigDecimal previousBalance = account.getBalance();
        BigDecimal newBalance = previousBalance.subtract(request.getAmount());
        account.setBalance(newBalance);

        accountRepository.save(account);

        log.info("Debited {} from account {}. Previous: {}, New: {}, Ref: {}",
                request.getAmount(), account.getAccountNumber(), previousBalance, newBalance, request.getTransactionReference());

        return new BalanceUpdateResponse(account.getAccountNumber(), previousBalance, newBalance,
                request.getTransactionReference(), "SUCCESS");
    }

    /**
     * Atomically credits the account using database-level pessimistic write locking (SELECT ... FOR UPDATE).
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BalanceUpdateResponse creditAccount(BalanceUpdateRequest request) {
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Credit amount must be strictly greater than 0");
        }

        Account account = accountRepository.findWithPessimisticLockByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountNumber", request.getAccountNumber()));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidTransactionException("Cannot credit inactive account " + request.getAccountNumber() +
                    " (Status: " + account.getStatus() + ")");
        }

        BigDecimal previousBalance = account.getBalance();
        BigDecimal newBalance = previousBalance.add(request.getAmount());
        account.setBalance(newBalance);

        accountRepository.save(account);

        log.info("Credited {} to account {}. Previous: {}, New: {}, Ref: {}",
                request.getAmount(), account.getAccountNumber(), previousBalance, newBalance, request.getTransactionReference());

        return new BalanceUpdateResponse(account.getAccountNumber(), previousBalance, newBalance,
                request.getTransactionReference(), "SUCCESS");
    }

    private void validateOwnership(Long customerId) {
        Optional<AuthenticatedUser> currentUserOpt = SecurityHelper.getCurrentUser();
        if (currentUserOpt.isEmpty()) {
            return; // Internal call / test mock
        }

        if (SecurityHelper.isAdminOrSupport()) {
            return; // Admins and Support staff have elevated access
        }

        // Customer can only view their own accounts
        AuthenticatedUser currentUser = currentUserOpt.get();
        if (currentUser.getId() != null && !currentUser.getId().equals(customerId)) {
            throw new ForbiddenException("Access denied: You do not have permission to view accounts for customer ID " + customerId);
        }
    }

    private String generateAccountNumber() {
        String accountNumber;
        do {
            accountNumber = "ACC" + (1000000000L + (long)(random.nextDouble() * 9000000000L));
        } while (accountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
    }

    private AccountResponseDto mapToDto(Account account) {
        return AccountResponseDto.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .customerId(account.getCustomerId())
                .accountType(account.getAccountType().name())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .status(account.getStatus().name())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}
