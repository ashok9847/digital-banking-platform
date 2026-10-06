package com.banking.account.repository;

import com.banking.account.entity.Account;
import com.banking.account.entity.AccountLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountLimitRepository extends JpaRepository<AccountLimit, Long> {

    Optional<AccountLimit> findByAccount(Account account);
}
