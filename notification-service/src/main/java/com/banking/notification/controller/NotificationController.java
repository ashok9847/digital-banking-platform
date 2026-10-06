package com.banking.notification.controller;

import com.banking.common.dto.ApiResponse;
import com.banking.notification.dto.NotificationResponseDto;
import com.banking.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Customer Notifications", description = "Transaction and account notification query endpoints")
@SecurityRequirement(name = "BearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get customer notifications", description = "Retrieves chronological notifications dispatched to a customer")
    public ResponseEntity<ApiResponse<List<NotificationResponseDto>>> getNotificationsByCustomerId(@PathVariable Long customerId) {
        List<NotificationResponseDto> notifications = notificationService.getNotificationsByCustomerId(customerId);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get notification by ID", description = "Retrieves specific notification delivery metadata")
    public ResponseEntity<ApiResponse<NotificationResponseDto>> getNotificationById(@PathVariable Long id) {
        NotificationResponseDto notification = notificationService.getNotificationById(id);
        return ResponseEntity.ok(ApiResponse.success(notification));
    }
}
