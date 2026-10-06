package com.banking.customer.controller;

import com.banking.common.dto.ApiResponse;
import com.banking.customer.dto.*;
import com.banking.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@Tag(name = "Customer Management", description = "Customer onboarding, KYC profiles, and customer administration")
@SecurityRequirement(name = "BearerAuth")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @Operation(summary = "Create customer profile", description = "Onboards a new customer linked to an authenticated user ID")
    public ResponseEntity<ApiResponse<CustomerResponseDto>> createCustomer(@Valid @RequestBody CustomerCreateRequest request) {
        CustomerResponseDto created = customerService.createCustomer(request);
        return new ResponseEntity<>(ApiResponse.success("Customer profile created successfully", created), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get customer by ID", description = "Retrieves customer profile by internal ID (owner or admin/support)")
    public ResponseEntity<ApiResponse<CustomerResponseDto>> getCustomerById(@PathVariable Long id) {
        CustomerResponseDto customer = customerService.getCustomerById(id);
        return ResponseEntity.ok(ApiResponse.success(customer));
    }

    @GetMapping("/number/{customerNumber}")
    @Operation(summary = "Get customer by customer number", description = "Retrieves customer profile by generated customer number")
    public ResponseEntity<ApiResponse<CustomerResponseDto>> getCustomerByNumber(@PathVariable String customerNumber) {
        CustomerResponseDto customer = customerService.getCustomerByNumber(customerNumber);
        return ResponseEntity.ok(ApiResponse.success(customer));
    }

    @GetMapping("/by-user/{userId}")
    @Operation(summary = "Get customer by user ID", description = "Retrieves customer profile linked to an auth user account")
    public ResponseEntity<ApiResponse<CustomerResponseDto>> getCustomerByUserId(@PathVariable Long userId) {
        CustomerResponseDto customer = customerService.getCustomerByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(customer));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update customer profile", description = "Updates personal contact details and address")
    public ResponseEntity<ApiResponse<CustomerResponseDto>> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CustomerUpdateRequest request) {
        CustomerResponseDto updated = customerService.updateCustomer(id, request);
        return ResponseEntity.ok(ApiResponse.success("Customer profile updated successfully", updated));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT')")
    @Operation(summary = "Change customer status", description = "Activate or deactivate a customer (Admin/Support only)")
    public ResponseEntity<ApiResponse<CustomerResponseDto>> updateCustomerStatus(
            @PathVariable Long id,
            @Valid @RequestBody CustomerStatusUpdateRequest request) {
        CustomerResponseDto updated = customerService.updateCustomerStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Customer status updated successfully", updated));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT')")
    @Operation(summary = "Search customers", description = "Search and paginate customers by name, email, or customer number (Admin/Support only)")
    public ResponseEntity<ApiResponse<Page<CustomerResponseDto>>> searchCustomers(
            @RequestParam(required = false) String query,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<CustomerResponseDto> customers = customerService.searchCustomers(query, pageable);
        return ResponseEntity.ok(ApiResponse.success(customers));
    }
}
