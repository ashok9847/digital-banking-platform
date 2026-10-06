package com.banking.customer.service;

import com.banking.common.exception.DuplicateResourceException;
import com.banking.common.exception.ResourceNotFoundException;
import com.banking.customer.dto.*;
import com.banking.customer.entity.Customer;
import com.banking.customer.entity.CustomerStatus;
import com.banking.customer.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    private Customer sampleCustomer;

    @BeforeEach
    void setUp() {
        sampleCustomer = new Customer(
                "CUST-12345678",
                100L,
                "Robert",
                "Johnson",
                "robert@banking.com",
                "+1234567890",
                LocalDate.of(1990, 5, 15),
                "123 Financial District, NY"
        );
        sampleCustomer.setId(1L);
    }

    @Test
    @DisplayName("Should successfully create a customer profile")
    void createCustomer_Success() {
        CustomerCreateRequest request = new CustomerCreateRequest(
                100L, "Robert", "Johnson", "robert@banking.com",
                "+1234567890", LocalDate.of(1990, 5, 15), "123 Financial District, NY"
        );

        when(customerRepository.existsByUserId(100L)).thenReturn(false);
        when(customerRepository.existsByEmail("robert@banking.com")).thenReturn(false);
        when(customerRepository.existsByCustomerNumber(any())).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(sampleCustomer);

        CustomerResponseDto response = customerService.createCustomer(request);

        assertNotNull(response);
        assertEquals("Robert", response.getFirstName());
        assertEquals("robert@banking.com", response.getEmail());
        assertEquals(100L, response.getUserId());
        assertEquals("ACTIVE", response.getStatus());
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException if user already has a customer profile")
    void createCustomer_DuplicateUser_ThrowsException() {
        CustomerCreateRequest request = new CustomerCreateRequest(
                100L, "Robert", "Johnson", "robert@banking.com",
                "+1234567890", LocalDate.of(1990, 5, 15), "123 Financial District, NY"
        );

        when(customerRepository.existsByUserId(100L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> customerService.createCustomer(request));
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    @DisplayName("Should retrieve customer by ID")
    void getCustomerById_Success() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));

        CustomerResponseDto response = customerService.getCustomerById(1L);

        assertNotNull(response);
        assertEquals("CUST-12345678", response.getCustomerNumber());
        assertEquals("Robert", response.getFirstName());
    }

    @Test
    @DisplayName("Should update customer status")
    void updateCustomerStatus_Success() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(customerRepository.save(any(Customer.class))).thenReturn(sampleCustomer);

        CustomerStatusUpdateRequest request = new CustomerStatusUpdateRequest(CustomerStatus.SUSPENDED);
        CustomerResponseDto response = customerService.updateCustomerStatus(1L, request);

        assertNotNull(response);
        assertEquals(CustomerStatus.SUSPENDED, sampleCustomer.getStatus());
    }
}
