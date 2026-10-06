package com.banking.common.event;

public final class EventTopics {

    private EventTopics() {}

    public static final String TRANSACTION_CREATED = "bank.transaction.created";
    public static final String TRANSACTION_COMPLETED = "bank.transaction.completed";
    public static final String TRANSACTION_FAILED = "bank.transaction.failed";
    public static final String NOTIFICATION_REQUESTED = "bank.notification.requested";
    public static final String AUDIT_CREATED = "bank.audit.created";
    public static final String TRANSACTION_COMPLETED_DLT = "bank.transaction.completed.DLT";
    public static final String TRANSACTION_CREATED_DLT = "bank.transaction.created.DLT";
    public static final String TRANSACTION_FAILED_DLT = "bank.transaction.failed.DLT";
    public static final String NOTIFICATION_REQUESTED_DLT = "bank.notification.requested.DLT";
    public static final String AUDIT_CREATED_DLT = "bank.audit.created.DLT";
}
