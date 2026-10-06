package com.banking.notification.service;

import com.banking.common.event.NotificationEvent;
import com.banking.common.event.TransactionEvent;
import com.banking.notification.dto.NotificationResponseDto;
import com.banking.notification.entity.Notification;
import com.banking.notification.entity.NotificationStatus;
import com.banking.notification.entity.NotificationType;
import com.banking.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private Notification sampleNotification;

    @BeforeEach
    void setUp() {
        sampleNotification = new Notification(
                10L,
                "TXN-123456",
                NotificationType.EMAIL,
                "customer@banking.com",
                "Transaction Alert",
                "Transfer of $100 completed",
                NotificationStatus.SENT
        );
        sampleNotification.setId(1L);
    }

    @Test
    @DisplayName("Should process transaction event and persist notification")
    void processTransactionEvent_Success() {
        TransactionEvent event = TransactionEvent.builder()
                .eventId("EVT-001")
                .transactionReference("TXN-123456")
                .customerId(10L)
                .sourceAccountNumber("ACC100")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .transactionType("TRANSFER")
                .status("SUCCESS")
                .build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(sampleNotification);

        Notification result = notificationService.processTransactionEvent(event);

        assertNotNull(result);
        assertEquals(NotificationStatus.SENT, result.getStatus());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("Should prevent duplicate notification creation for same transaction reference")
    void processTransactionEvent_DuplicateEvent_ReturnsExistingWithoutSaving() {
        TransactionEvent event = TransactionEvent.builder()
                .eventId("EVT-DUP-001")
                .transactionReference("TXN-123456")
                .customerId(10L)
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .transactionType("TRANSFER")
                .status("SUCCESS")
                .build();

        when(notificationRepository.findFirstByTransactionReference("TXN-123456"))
                .thenReturn(Optional.of(sampleNotification));

        Notification result = notificationService.processTransactionEvent(event);

        assertNotNull(result);
        assertEquals(sampleNotification.getId(), result.getId());
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    @DisplayName("Should process direct notification event")
    void processDirectNotification_Success() {
        NotificationEvent event = NotificationEvent.builder()
                .eventId("EVT-DIR-001")
                .customerId(10L)
                .notificationType("SMS")
                .recipient("+15550001111")
                .subject("Security alert")
                .message("New login from unrecognized IP")
                .build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(sampleNotification);

        Notification result = notificationService.processDirectNotification(event);

        assertNotNull(result);
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("Should retrieve notifications by customer ID")
    void getNotificationsByCustomerId_Success() {
        when(notificationRepository.findByCustomerIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(sampleNotification));

        List<NotificationResponseDto> list = notificationService.getNotificationsByCustomerId(10L);

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("TXN-123456", list.get(0).getTransactionReference());
    }
}
