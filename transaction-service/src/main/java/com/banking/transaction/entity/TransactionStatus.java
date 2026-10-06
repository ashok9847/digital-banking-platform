package com.banking.transaction.entity;

public enum TransactionStatus {
    PENDING {
        @Override
        public boolean canTransitionTo(TransactionStatus target) {
            return target == SUCCESS || target == FAILED;
        }
    },
    SUCCESS {
        @Override
        public boolean canTransitionTo(TransactionStatus target) {
            return target == REVERSED;
        }
    },
    FAILED {
        @Override
        public boolean canTransitionTo(TransactionStatus target) {
            return false; // Terminal state
        }
    },
    REVERSED {
        @Override
        public boolean canTransitionTo(TransactionStatus target) {
            return false; // Terminal state
        }
    };

    public abstract boolean canTransitionTo(TransactionStatus target);

    public void validateTransitionTo(TransactionStatus target) {
        if (!canTransitionTo(target)) {
            throw new IllegalStateException(
                    String.format("Illegal transaction state transition: cannot transition from %s to %s", this, target)
            );
        }
    }
}
