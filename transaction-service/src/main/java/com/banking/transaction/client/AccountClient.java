package com.banking.transaction.client;

import java.math.BigDecimal;

public interface AccountClient {

    AccountDto getAccount(String accountNumber, String bearerToken);

    BalanceResultDto debit(String accountNumber, BigDecimal amount, String reference);

    BalanceResultDto credit(String accountNumber, BigDecimal amount, String reference);
}
