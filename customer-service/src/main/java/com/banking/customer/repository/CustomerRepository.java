package com.banking.customer.repository;

import com.banking.customer.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByCustomerNumber(String customerNumber);

    Optional<Customer> findByUserId(Long userId);

    Optional<Customer> findByEmail(String email);

    boolean existsByCustomerNumber(String customerNumber);

    boolean existsByUserId(Long userId);

    boolean existsByEmail(String email);

    @Query("SELECT c FROM Customer c WHERE " +
           "(:query IS NULL OR LOWER(c.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.customerNumber) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Customer> searchCustomers(@Param("query") String query, Pageable pageable);
}
