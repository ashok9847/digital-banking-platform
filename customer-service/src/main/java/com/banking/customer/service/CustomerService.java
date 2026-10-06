package com.banking.customer.service;

import com.banking.common.exception.DuplicateResourceException;
import com.banking.common.exception.ForbiddenException;
import com.banking.common.exception.ResourceNotFoundException;
import com.banking.common.security.AuthenticatedUser;
import com.banking.common.security.SecurityHelper;
import com.banking.customer.dto.*;
import com.banking.customer.entity.Customer;
import com.banking.customer.entity.CustomerStatus;
import com.banking.customer.repository.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Optional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final SecureRandom random = new SecureRandom();

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public CustomerResponseDto createCustomer(CustomerCreateRequest request) {
        if (customerRepository.existsByUserId(request.getUserId())) {
            throw new DuplicateResourceException("Customer profile", "userId", request.getUserId());
        }

        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Customer profile", "email", request.getEmail());
        }

        String customerNumber = generateCustomerNumber();

        Customer customer = new Customer(
                customerNumber,
                request.getUserId(),
                request.getFirstName(),
                request.getLastName(),
                request.getEmail(),
                request.getPhone(),
                request.getDateOfBirth(),
                request.getAddress()
        );

        Customer saved = customerRepository.save(customer);
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public CustomerResponseDto getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));
        validateAccess(customer.getUserId());
        return mapToDto(customer);
    }

    @Transactional(readOnly = true)
    public CustomerResponseDto getCustomerByNumber(String customerNumber) {
        Customer customer = customerRepository.findByCustomerNumber(customerNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "customerNumber", customerNumber));
        validateAccess(customer.getUserId());
        return mapToDto(customer);
    }

    @Transactional(readOnly = true)
    public CustomerResponseDto getCustomerByUserId(Long userId) {
        validateAccess(userId);
        Customer customer = customerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "userId", userId));
        return mapToDto(customer);
    }

    @Transactional
    public CustomerResponseDto updateCustomer(Long id, CustomerUpdateRequest request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));

        validateAccess(customer.getUserId());

        if (!customer.getEmail().equalsIgnoreCase(request.getEmail()) &&
                customerRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Customer profile", "email", request.getEmail());
        }

        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setAddress(request.getAddress());

        Customer updated = customerRepository.save(customer);
        return mapToDto(updated);
    }

    @Transactional
    public CustomerResponseDto updateCustomerStatus(Long id, CustomerStatusUpdateRequest request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));

        customer.setStatus(request.getStatus());
        Customer updated = customerRepository.save(customer);
        return mapToDto(updated);
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponseDto> searchCustomers(String query, Pageable pageable) {
        return customerRepository.searchCustomers(query, pageable).map(this::mapToDto);
    }

    private void validateAccess(Long targetUserId) {
        Optional<AuthenticatedUser> currentUserOpt = SecurityHelper.getCurrentUser();
        if (currentUserOpt.isEmpty()) {
            return; // allowed for internal / mocked requests if unauthenticated
        }

        AuthenticatedUser currentUser = currentUserOpt.get();
        boolean isPrivileged = SecurityHelper.isAdminOrSupport();

        if (!isPrivileged && (currentUser.getId() == null || !currentUser.getId().equals(targetUserId))) {
            throw new ForbiddenException("You are not authorized to view or modify this customer profile");
        }
    }

    private String generateCustomerNumber() {
        String number;
        do {
            number = "CUST-" + (10000000 + random.nextInt(90000000));
        } while (customerRepository.existsByCustomerNumber(number));
        return number;
    }

    private CustomerResponseDto mapToDto(Customer customer) {
        return CustomerResponseDto.builder()
                .id(customer.getId())
                .customerNumber(customer.getCustomerNumber())
                .userId(customer.getUserId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .dateOfBirth(customer.getDateOfBirth())
                .address(customer.getAddress())
                .status(customer.getStatus().name())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }
}
