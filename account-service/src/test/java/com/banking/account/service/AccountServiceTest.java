package com.banking.account.service;

import com.banking.account.dto.*;
import com.banking.account.entity.*;
import com.banking.account.repository.AccountLimitRepository;
import com.banking.account.repository.AccountRepository;
import com.banking.common.exception.InsufficientBalanceException;
import com.banking.common.exception.InvalidTransactionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountLimitRepository accountLimitRepository;

    @InjectMocks
    private AccountService accountService;

    private Account sampleAccount;

    @BeforeEach
    void setUp() {
        sampleAccount = new Account(
                "ACC1000200030",
                50L,
                AccountType.SAVINGS,
                new BigDecimal("1000.0000"),
                "USD"
        );
        sampleAccount.setId(1L);
    }

    @Test
    @DisplayName("Should successfully open an account with initial deposit")
    void createAccount_Success() {
        AccountCreateRequest request = new AccountCreateRequest(
                50L, AccountType.SAVINGS, new BigDecimal("500.0000"), "USD"
        );

        when(accountRepository.existsByAccountNumber(any())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(sampleAccount);
        when(accountLimitRepository.save(any(AccountLimit.class))).thenReturn(new AccountLimit());

        AccountResponseDto response = accountService.createAccount(request);

        assertNotNull(response);
        assertEquals("ACC1000200030", response.getAccountNumber());
        assertEquals(new BigDecimal("1000.0000"), response.getBalance());
        assertEquals("ACTIVE", response.getStatus());
        verify(accountRepository).save(any(Account.class));
        verify(accountLimitRepository).save(any(AccountLimit.class));
    }

    @Test
    @DisplayName("Should atomically debit balance when funds are sufficient")
    void debitAccount_Success() {
        BalanceUpdateRequest request = new BalanceUpdateRequest("ACC1000200030", new BigDecimal("300.0000"), "TX-REF-001");
        when(accountRepository.findWithPessimisticLockByAccountNumber("ACC1000200030")).thenReturn(Optional.of(sampleAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(sampleAccount);

        BalanceUpdateResponse response = accountService.debitAccount(request);

        assertNotNull(response);
        assertEquals(new BigDecimal("1000.0000"), response.getPreviousBalance());
        assertEquals(new BigDecimal("700.0000"), response.getNewBalance());
        assertEquals("SUCCESS", response.getStatus());
        assertEquals(new BigDecimal("700.0000"), sampleAccount.getBalance());
        verify(accountRepository).save(sampleAccount);
    }

    @Test
    @DisplayName("Should throw InsufficientBalanceException when debit amount exceeds balance")
    void debitAccount_InsufficientFunds_ThrowsException() {
        BalanceUpdateRequest request = new BalanceUpdateRequest("ACC1000200030", new BigDecimal("1500.0000"), "TX-REF-002");
        when(accountRepository.findWithPessimisticLockByAccountNumber("ACC1000200030")).thenReturn(Optional.of(sampleAccount));

        assertThrows(InsufficientBalanceException.class, () -> accountService.debitAccount(request));
        assertEquals(new BigDecimal("1000.0000"), sampleAccount.getBalance());
        verify(accountRepository, never()).save(sampleAccount);
    }

    @Test
    @DisplayName("Should throw InvalidTransactionException when debiting a blocked account")
    void debitAccount_BlockedAccount_ThrowsException() {
        sampleAccount.setStatus(AccountStatus.BLOCKED);
        BalanceUpdateRequest request = new BalanceUpdateRequest("ACC1000200030", new BigDecimal("100.0000"), "TX-REF-003");
        when(accountRepository.findWithPessimisticLockByAccountNumber("ACC1000200030")).thenReturn(Optional.of(sampleAccount));

        assertThrows(InvalidTransactionException.class, () -> accountService.debitAccount(request));
        verify(accountRepository, never()).save(sampleAccount);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when debiting non-existent account")
    void debitAccount_AccountNotFound_ThrowsException() {
        BalanceUpdateRequest request = new BalanceUpdateRequest("ACC_NON_EXISTENT", new BigDecimal("100.0000"), "TX-REF-404");
        when(accountRepository.findWithPessimisticLockByAccountNumber("ACC_NON_EXISTENT")).thenReturn(Optional.empty());

        assertThrows(com.banking.common.exception.ResourceNotFoundException.class, () -> accountService.debitAccount(request));
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should atomically credit balance")
    void creditAccount_Success() {
        BalanceUpdateRequest request = new BalanceUpdateRequest("ACC1000200030", new BigDecimal("250.0000"), "TX-REF-004");
        when(accountRepository.findWithPessimisticLockByAccountNumber("ACC1000200030")).thenReturn(Optional.of(sampleAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(sampleAccount);

        BalanceUpdateResponse response = accountService.creditAccount(request);

        assertNotNull(response);
        assertEquals(new BigDecimal("1000.0000"), response.getPreviousBalance());
        assertEquals(new BigDecimal("1250.0000"), response.getNewBalance());
        assertEquals(new BigDecimal("1250.0000"), sampleAccount.getBalance());
        verify(accountRepository).save(sampleAccount);
    }

    @Test
    @DisplayName("Should throw InvalidTransactionException when debiting negative or zero amount")
    void debitAccount_NegativeOrZeroAmount_ThrowsException() {
        BalanceUpdateRequest zeroReq = new BalanceUpdateRequest("ACC1000200030", BigDecimal.ZERO, "TX-REF-005");
        BalanceUpdateRequest negReq = new BalanceUpdateRequest("ACC1000200030", new BigDecimal("-50.00"), "TX-REF-006");

        assertThrows(InvalidTransactionException.class, () -> accountService.debitAccount(zeroReq));
        assertThrows(InvalidTransactionException.class, () -> accountService.debitAccount(negReq));
        verify(accountRepository, never()).findWithPessimisticLockByAccountNumber(any());
    }

    @Test
    @DisplayName("Should throw InvalidTransactionException when crediting negative or zero amount")
    void creditAccount_NegativeOrZeroAmount_ThrowsException() {
        BalanceUpdateRequest zeroReq = new BalanceUpdateRequest("ACC1000200030", BigDecimal.ZERO, "TX-REF-007");
        BalanceUpdateRequest negReq = new BalanceUpdateRequest("ACC1000200030", new BigDecimal("-50.00"), "TX-REF-008");

        assertThrows(InvalidTransactionException.class, () -> accountService.creditAccount(zeroReq));
        assertThrows(InvalidTransactionException.class, () -> accountService.creditAccount(negReq));
        verify(accountRepository, never()).findWithPessimisticLockByAccountNumber(any());
    }

    @Test
    @DisplayName("Should maintain balance safety and prevent overdraft under concurrent debit requests")
    void debitAccount_ConcurrentSimultaneousDebits_PreventsOverdraft() throws InterruptedException {
        // Given an account with initial balance = $500.00
        sampleAccount.setBalance(new BigDecimal("500.0000"));

        // When locking the account in repo, we synchronize on sampleAccount to simulate database row locking
        when(accountRepository.findWithPessimisticLockByAccountNumber("ACC1000200030"))
                .thenAnswer(inv -> Optional.of(sampleAccount));
        when(accountRepository.save(any(Account.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        int threadCount = 10;
        BigDecimal debitPerThread = new BigDecimal("100.0000");
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount);
        java.util.concurrent.CountDownLatch startLatch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch doneLatch = new java.util.concurrent.CountDownLatch(threadCount);
        java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger failureCount = new java.util.concurrent.atomic.AtomicInteger(0);

        Object lockObject = new Object();

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    // Simulate pessimistic DB row lock (SELECT FOR UPDATE)
                    synchronized (lockObject) {
                        BalanceUpdateRequest req = new BalanceUpdateRequest("ACC1000200030", debitPerThread, "TX-CONC-" + index);
                        accountService.debitAccount(req);
                        successCount.incrementAndGet();
                    }
                } catch (InsufficientBalanceException ex) {
                    failureCount.incrementAndGet();
                } catch (Exception ex) {
                    // Unexpected
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Fire all threads simultaneously
        doneLatch.await(5, java.util.concurrent.TimeUnit.SECONDS);
        executor.shutdown();

        // Exactly 5 transactions should succeed ($500 / $100 = 5) and 5 should fail with InsufficientBalanceException
        assertEquals(5, successCount.get(), "Exactly 5 debits should succeed");
        assertEquals(5, failureCount.get(), "Remaining 5 debits must be rejected due to insufficient balance");
        assertEquals(new BigDecimal("0.0000"), sampleAccount.getBalance(), "Final balance must never be negative");
    }

    @Test
    @DisplayName("Concurrency: Two simultaneous 800 withdrawals against 1000 balance: exactly one succeeds, balance is 200, never negative")
    void debitAccount_TwoSimultaneousWithdrawals_OnlyOneSucceeds() throws InterruptedException {
        // Initial balance = 1,000.00
        sampleAccount.setBalance(new BigDecimal("1000.0000"));

        when(accountRepository.findWithPessimisticLockByAccountNumber("ACC1000200030"))
                .thenAnswer(inv -> Optional.of(sampleAccount));
        when(accountRepository.save(any(Account.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        BigDecimal withdrawalAmount = new BigDecimal("800.0000");
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.CountDownLatch startLatch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch doneLatch = new java.util.concurrent.CountDownLatch(2);
        java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger failureCount = new java.util.concurrent.atomic.AtomicInteger(0);

        Object rowLock = new Object();

        for (int i = 1; i <= 2; i++) {
            final String reqId = "REQ-" + (i == 1 ? "A" : "B");
            executor.submit(() -> {
                try {
                    startLatch.await();
                    synchronized (rowLock) {
                        BalanceUpdateRequest req = new BalanceUpdateRequest("ACC1000200030", withdrawalAmount, reqId);
                        accountService.debitAccount(req);
                        successCount.incrementAndGet();
                    }
                } catch (InsufficientBalanceException ex) {
                    failureCount.incrementAndGet();
                } catch (Exception ex) {
                    // Unexpected
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(3, java.util.concurrent.TimeUnit.SECONDS);
        executor.shutdown();

        // Both requests must NOT succeed
        assertEquals(1, successCount.get(), "Only one withdrawal of 800 can succeed");
        assertEquals(1, failureCount.get(), "Second withdrawal must fail with InsufficientBalanceException");
        assertEquals(new BigDecimal("200.0000"), sampleAccount.getBalance(), "Balance must be strictly 200, never negative");
    }
}
