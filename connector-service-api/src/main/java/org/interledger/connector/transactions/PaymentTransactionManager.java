package org.interledger.connector.transactions;

import org.interledger.connector.accounts.AccountId;

import org.springframework.data.domain.PageRequest;

import java.util.List;

public interface PaymentTransactionManager {

  List<Transaction> findByAccountId(AccountId accountId, PageRequest pageRequest);

  void upsert(Transaction transaction);

}
