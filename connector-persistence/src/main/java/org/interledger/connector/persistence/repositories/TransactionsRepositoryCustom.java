package org.interledger.connector.persistence.repositories;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.persistence.entities.TransactionEntity;
import org.interledger.connector.transactions.TransactionStatus;

/**
 * Custom persistence operations for {@link TransactionEntity} that, for performance, are done
 * via JDBC statements instead of using {@link TransactionsRepository}.
 */
public interface TransactionsRepositoryCustom {

  /**
   * Inserts or updates an transaction entity and merges amounts.
   * @param transaction
   * @return number of rows updated. 1 = success.
   */
  int upsertAmounts(TransactionEntity transaction);

  /**
   * Updates just the status column of the transactions table. This is done explicitly because
   * upsertAmounts does not update the status because status changes require an explicit event such
   * as a stream close frame being sent, or a transaction being rolled off based on time expiry.
   * @param accountId
   * @param transactionId
   * @param status
   * @return number of rows updated. 1 = success.
   */
  int updateStatus(AccountId accountId, String transactionId, TransactionStatus status);


  /**
   * Updates just the source_address column of the transactions table. This is done explicitly because
   * source address will not be present on most packets, just when the ILP stream data contains a ConnectionNewAddress
   * frame.
   * @param accountId
   * @param transactionId
   * @param sourceAddress
   * @return number of rows updated. 1 = success
   */
  int updateSourceAddress(AccountId accountId, String transactionId, String sourceAddress);

}
