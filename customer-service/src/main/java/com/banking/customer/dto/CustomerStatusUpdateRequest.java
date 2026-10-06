package com.banking.customer.dto;

import com.banking.customer.entity.CustomerStatus;
import jakarta.validation.constraints.NotNull;

public class CustomerStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private CustomerStatus status;

    public CustomerStatusUpdateRequest() {}

    public CustomerStatusUpdateRequest(CustomerStatus status) {
        this.status = status;
    }

    public CustomerStatus getStatus() { return status; }
    public void setStatus(CustomerStatus status) { this.status = status; }
}
