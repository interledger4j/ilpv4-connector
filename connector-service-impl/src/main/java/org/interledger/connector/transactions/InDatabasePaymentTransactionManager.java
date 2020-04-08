package org.interledger.connector.transactions;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.persistence.repositories.TransactionsRepository;

import com.google.common.base.Preconditions;
import org.springframework.data.domain.PageRequest;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link PaymentTransactionManager} that persists to a SQL database.
 */
public class InDatabasePaymentTransactionManager implements PaymentTransactionManager {

  private final TransactionsRepository transactionsRepository;

  private final TransactionFromEntityConverter transactionFromEntityConverter;

  private final TransactionToEntityConverter transactionToEntityConverter;

  public InDatabasePaymentTransactionManager(TransactionsRepository transactionsRepository,
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
  public Optional<Transaction> findByAccountIdAndTransactionId(AccountId accountId, String transactionId) {
    return transactionsRepository.findByAccountIdAndTransactionId(accountId, transactionId)
      .map(transactionFromEntityConverter::convert);
  }

  @Override
  public void merge(Transaction transaction) {
    validateAmount(transaction.amount(), transaction.type().getAdjustmentType());
    transactionsRepository.upsertAmounts(transactionToEntityConverter.convert(transaction));
    transaction.sourceAddress().ifPresent(sourceAddress -> {
      transactionsRepository.updateSourceAddress(transaction.accountId(),
        transaction.transactionId(),
        sourceAddress.getValue());
    });
    if (!transaction.status().equals(TransactionStatus.PENDING)) {
      transactionsRepository.updateStatus(transaction.accountId(),
        transaction.transactionId(),
        transaction.status());
    }
  }

  /**
   * Validates the amount is correctly negative or positive based on CREDIT vs DEBIT types.
   * @param amount
   * @param balanceAdjustmentType
   */
  private static void validateAmount(BigInteger amount, TransactionType.BalanceAdjustmentType balanceAdjustmentType) {
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
