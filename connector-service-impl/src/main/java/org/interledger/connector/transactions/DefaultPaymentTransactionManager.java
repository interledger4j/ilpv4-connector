package org.interledger.connector.transactions;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.persistence.repositories.TransactionsRepository;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedLong;
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
    validateAmount(transaction.amount(), transaction.type().getAdjustmentType());
    transactionsRepository.upsertAmounts(transactionToEntityConverter.convert(transaction));
    transaction.sourceAddress().ifPresent(sourceAddress -> {
      transactionsRepository.updateSourceAddress(transaction.accountId(),
        transaction.referenceId(),
        sourceAddress.getValue());
    });
    if (!transaction.status().equals(TransactionStatus.PENDING)) {
      transactionsRepository.updateStatus(transaction.accountId(),
        transaction.referenceId(),
        transaction.status().toString());
    }
  }

  private void validateAmount(UnsignedLong amount, TransactionType.BalanceAdjustmentType balanceAdjustmentType) {
    switch (balanceAdjustmentType) {
      case CREDIT:
        Preconditions.checkArgument(amount.longValue() >= 0, "amount cannot be negative for a credit adjustment");
        break;
      case DEBIT:
        Preconditions.checkArgument(amount.longValue() <= 0, "amount cannot be positive for a debit adjustment");
        break;
    }
  }

}
