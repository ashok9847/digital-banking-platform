package com.banking.audit.controller;

import com.banking.audit.dto.AuditLogResponseDto;
import com.banking.audit.service.AuditService;
import com.banking.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@Tag(name = "Compliance & Audit Trail", description = "Regulatory audit query endpoints (Admin/Support only)")
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT')")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/logs")
    @Operation(summary = "Search audit logs", description = "Query immutable audit logs with optional actor, action, and resourceType filters")
    public ResponseEntity<ApiResponse<Page<AuditLogResponseDto>>> searchLogs(
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<AuditLogResponseDto> logs = auditService.searchAuditLogs(actor, action, resourceType, pageable);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/resource/{resourceType}/{resourceId}")
    @Operation(summary = "Get resource audit history", description = "Retrieves complete chronological audit trail for a specific entity")
    public ResponseEntity<ApiResponse<List<AuditLogResponseDto>>> getResourceHistory(
            @PathVariable String resourceType,
            @PathVariable String resourceId) {
        List<AuditLogResponseDto> history = auditService.getResourceHistory(resourceType, resourceId);
        return ResponseEntity.ok(ApiResponse.success(history));
    }
}
