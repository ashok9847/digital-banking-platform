package com.banking.account.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "account_limits")
public class AccountLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private Account account;

    @Column(name = "daily_transfer_limit", nullable = false, precision = 19, scale = 4)
    private BigDecimal dailyTransferLimit = new BigDecimal("50000.0000");

    @Column(name = "single_tx_limit", nullable = false, precision = 19, scale = 4)
    private BigDecimal singleTxLimit = new BigDecimal("10000.0000");

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public AccountLimit() {}

    public AccountLimit(Account account, BigDecimal dailyTransferLimit, BigDecimal singleTxLimit) {
        this.account = account;
        this.dailyTransferLimit = dailyTransferLimit;
        this.singleTxLimit = singleTxLimit;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }

    public BigDecimal getDailyTransferLimit() { return dailyTransferLimit; }
    public void setDailyTransferLimit(BigDecimal dailyTransferLimit) { this.dailyTransferLimit = dailyTransferLimit; }

    public BigDecimal getSingleTxLimit() { return singleTxLimit; }
    public void setSingleTxLimit(BigDecimal singleTxLimit) { this.singleTxLimit = singleTxLimit; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
