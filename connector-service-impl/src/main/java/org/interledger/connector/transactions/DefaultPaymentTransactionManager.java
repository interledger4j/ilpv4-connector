package org.interledger.connector.transactions;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.persistence.repositories.TransactionsRepository;

import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.stream.Collectors;

public class DefaultPaymentTransactionManager implements PaymentTransactionManager {

  private final TransactionsRepository transactionsRepository;

  private final TransactionFromEntityConverter transactionFromEntityConverter;

  private final TransactionToEntityConverter transactionToEntityConverter;

  public DefaultPaymentTransactionManager(TransactionsRepository transactionsRepository,
                                          TransactionFromEntityConverter transactionFromEntityConverter,
                                          TransactionToEntityConverter transactionToEntityConverter) {
    this.transactionsRepository = transactionsRepository;
    this.transactionFromEntityConverter = transactionFromEntityConverter;
    this.transactionToEntityConverter = transactionToEntityConverter;
  }

  @Override
  public List<Transaction> findByAccountId(AccountId accountId, PageRequest pageRequest) {
    return transactionsRepository.findByAccountIdOrderByCreatedDateDesc(accountId, pageRequest)
      .stream()
      .map(transactionFromEntityConverter::convert)
      .collect(Collectors.toList());
  }

  @Override
  public void upsert(Transaction transaction) {
    if (transaction.amount().longValue() > 0) {
      transactionsRepository.upsertAmounts(transactionToEntityConverter.convert(transaction));
    }
    if (!transaction.transactionStatus().equals(TransactionStatus.PENDING)) {
      transactionsRepository.updateStatus(transaction.accountId(),
        transaction.referenceId(),
        transaction.transactionStatus().toString());
    }
  }
}
