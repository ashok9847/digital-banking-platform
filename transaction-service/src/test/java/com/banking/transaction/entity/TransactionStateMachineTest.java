package com.banking.transaction.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class TransactionStateMachineTest {

    private Transaction transaction;

    @BeforeEach
    void setUp() {
        transaction = new Transaction(
                "TXN-STATE-001",
                "idemp-state-001",
                "ACC100",
                "ACC200",
                new BigDecimal("100.00"),
                "USD",
                TransactionType.TRANSFER,
                TransactionStatus.PENDING,
                "State test"
        );
    }

    @Test
    @DisplayName("Should allow legal transition from PENDING to SUCCESS")
    void testPendingToSuccess_Allowed() {
        transaction.transitionTo(TransactionStatus.SUCCESS);
        assertEquals(TransactionStatus.SUCCESS, transaction.getStatus());
    }

    @Test
    @DisplayName("Should allow legal transition from PENDING to FAILED")
    void testPendingToFailed_Allowed() {
        transaction.transitionTo(TransactionStatus.FAILED);
        assertEquals(TransactionStatus.FAILED, transaction.getStatus());
    }

    @Test
    @DisplayName("Should allow legal transition from SUCCESS to REVERSED")
    void testSuccessToReversed_Allowed() {
        transaction.transitionTo(TransactionStatus.SUCCESS);
        transaction.transitionTo(TransactionStatus.REVERSED);
        assertEquals(TransactionStatus.REVERSED, transaction.getStatus());
    }

    @Test
    @DisplayName("Should allow setting same status without error")
    void testSameStatus_Allowed() {
        transaction.transitionTo(TransactionStatus.PENDING);
        assertEquals(TransactionStatus.PENDING, transaction.getStatus());

        transaction.transitionTo(TransactionStatus.SUCCESS);
        transaction.transitionTo(TransactionStatus.SUCCESS);
        assertEquals(TransactionStatus.SUCCESS, transaction.getStatus());
    }

    @Test
    @DisplayName("Should forbid illegal transition from PENDING directly to REVERSED")
    void testPendingToReversed_Forbidden() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                transaction.transitionTo(TransactionStatus.REVERSED));
        assertTrue(ex.getMessage().contains("cannot transition from PENDING to REVERSED"));
    }

    @Test
    @DisplayName("Should forbid illegal transition from SUCCESS to PENDING or FAILED")
    void testSuccessIllegalTransitions_Forbidden() {
        transaction.transitionTo(TransactionStatus.SUCCESS);

        assertThrows(IllegalStateException.class, () -> transaction.transitionTo(TransactionStatus.PENDING));
        assertThrows(IllegalStateException.class, () -> transaction.transitionTo(TransactionStatus.FAILED));
    }

    @ParameterizedTest
    @EnumSource(value = TransactionStatus.class, names = {"PENDING", "SUCCESS", "REVERSED"})
    @DisplayName("Should forbid any transition out of terminal FAILED state")
    void testFailedIsTerminal(TransactionStatus target) {
        transaction.transitionTo(TransactionStatus.FAILED);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                transaction.transitionTo(target));
        assertTrue(ex.getMessage().contains("cannot transition from FAILED to " + target));
    }

    @ParameterizedTest
    @EnumSource(value = TransactionStatus.class, names = {"PENDING", "SUCCESS", "FAILED"})
    @DisplayName("Should forbid any transition out of terminal REVERSED state")
    void testReversedIsTerminal(TransactionStatus target) {
        transaction.transitionTo(TransactionStatus.SUCCESS);
        transaction.transitionTo(TransactionStatus.REVERSED);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                transaction.transitionTo(target));
        assertTrue(ex.getMessage().contains("cannot transition from REVERSED to " + target));
    }
}
