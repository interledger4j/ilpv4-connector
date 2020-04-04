package org.interledger.connector.transactions;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.persistence.entities.TransactionEntity;
import org.interledger.connector.persistence.repositories.TransactionsRepository;

import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;

public class DefaultPaymentTransactionManager implements PaymentTransactionManager {

  private final TransactionsRepository transactionsRepository;

  private final EntityManager entityManager;

  private final TransactionFromEntityConverter transactionFromEntityConverter;

  private final TransactionToEntityConverter transactionToEntityConverter;

  public DefaultPaymentTransactionManager(TransactionsRepository transactionsRepository,
                                          EntityManager entityManager,
                                          TransactionFromEntityConverter transactionFromEntityConverter,
                                          TransactionToEntityConverter transactionToEntityConverter) {
    this.transactionsRepository = transactionsRepository;
    this.entityManager = entityManager;
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
    // because upsert may cause the entity to be merged in the database, JPA will be unaware if the merges values
    // are different than what the entity had. To avoid being out of sync, we
    // 1. clear the session so that if the entity is already stale, JPA won't use cached values
    // 2. save and flush immediately to the database so that the upsert happens right away
    // 3. refresh from the database
    entityManager.clear();
    TransactionEntity result = transactionsRepository.save(transactionToEntityConverter.convert(transaction));
    entityManager.flush();
    entityManager.refresh(result);
    if (!transaction.transactionStatus().equals(TransactionStatus.PENDING)) {
      result.setStatus(transaction.transactionStatus().toString());
      entityManager.flush();
    }
  }

}
