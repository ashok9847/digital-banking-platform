package com.banking.notification.service;

import com.banking.common.event.NotificationEvent;
import com.banking.common.event.TransactionEvent;
import com.banking.common.exception.ResourceNotFoundException;
import com.banking.notification.dto.NotificationResponseDto;
import com.banking.notification.entity.Notification;
import com.banking.notification.entity.NotificationStatus;
import com.banking.notification.entity.NotificationType;
import com.banking.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public Notification processTransactionEvent(TransactionEvent event) {
        log.info("Processing transaction event for notification: ref={}, type={}, amount={}",
                event.getTransactionReference(), event.getTransactionType(), event.getAmount());

        if (event.getTransactionReference() != null && !event.getTransactionReference().isBlank()) {
            java.util.Optional<Notification> existing = notificationRepository.findFirstByTransactionReference(event.getTransactionReference());
            if (existing.isPresent()) {
                log.warn("Duplicate notification event received for txRef [{}]. Skipping duplicate notification creation.",
                        event.getTransactionReference());
                return existing.get();
            }
        }

        String subject = String.format("Bank Notification: %s %s", event.getTransactionType(), event.getStatus());
        String message = String.format("Dear customer, your %s of %s %s was processed with status: %s. Reference: %s",
                event.getTransactionType(), event.getAmount(), event.getCurrency(),
                event.getStatus(), event.getTransactionReference());

        String recipient = (event.getSourceAccountNumber() != null) ?
                "account-" + event.getSourceAccountNumber() + "@banking.internal" :
                "account-" + event.getDestinationAccountNumber() + "@banking.internal";

        Notification notification = new Notification(
                event.getCustomerId(),
                event.getTransactionReference(),
                NotificationType.EMAIL,
                recipient,
                subject,
                message,
                NotificationStatus.SENT
        );

        Notification saved = notificationRepository.save(notification);
        log.info("Notification dispatched successfully: id={}, recipient={}, type={}",
                saved.getId(), saved.getRecipient(), saved.getNotificationType());
        return saved;
    }

    @Transactional
    public Notification processDirectNotification(NotificationEvent event) {
        log.info("Processing direct notification event: eventId={}, customerId={}, type={}",
                event.getEventId(), event.getCustomerId(), event.getNotificationType());

        NotificationType type = NotificationType.EMAIL;
        try {
            if (event.getNotificationType() != null) {
                type = NotificationType.valueOf(event.getNotificationType().toUpperCase());
            }
        } catch (IllegalArgumentException e) {
            type = NotificationType.EMAIL;
        }

        Notification notification = new Notification(
                event.getCustomerId(),
                event.getTransactionReference(),
                type,
                event.getRecipient() != null ? event.getRecipient() : "customer@banking.internal",
                event.getSubject() != null ? event.getSubject() : "Bank Notification",
                event.getMessage(),
                NotificationStatus.SENT
        );

        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getNotificationsByCustomerId(Long customerId) {
        return notificationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NotificationResponseDto getNotificationById(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));
        return mapToDto(notification);
    }

    private NotificationResponseDto mapToDto(Notification n) {
        return NotificationResponseDto.builder()
                .id(n.getId())
                .customerId(n.getCustomerId())
                .transactionReference(n.getTransactionReference())
                .notificationType(n.getNotificationType().name())
                .recipient(n.getRecipient())
                .subject(n.getSubject())
                .message(n.getMessage())
                .status(n.getStatus().name())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
