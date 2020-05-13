package org.interledger.connector.persistence.repositories;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.payments.StreamPaymentStatus;
import org.interledger.connector.persistence.entities.StreamPaymentEntity;

/**
 * Custom persistence operations for {@link StreamPaymentEntity} that, for performance, are done
 * via JDBC statements instead of using {@link StreamPaymentsRepository}.
 */
public interface StreamPaymentsRepositoryCustom {

  /**
   * Inserts or updates an stream payment entity and merges amounts.
   * @param streamPayment
   * @return number of rows updated. 1 = success.
   */
  int upsertAmounts(StreamPaymentEntity streamPayment);

  /**
   * Updates just the status column of the payments table. This is done explicitly because
   * upsertAmounts does not update the status because status changes require an explicit event such
   * as a stream close frame being sent, or a streamPayment being rolled off based on time expiry.
   * @param accountId
   * @param streamPaymentId
   * @param status
   * @return number of rows updated. 1 = success.
   */
  int updateStatus(AccountId accountId, String streamPaymentId, StreamPaymentStatus status);


  /**
   * Updates just the source_address column of the payments table. This is done explicitly because
   * source address will not be present on most packets, just when the ILP stream data contains a ConnectionNewAddress
   * frame.
   * @param accountId
   * @param streamPaymentId
   * @param sourceAddress
   * @return number of rows updated. 1 = success
   */
  int updateSourceAddress(AccountId accountId, String streamPaymentId, String sourceAddress);

  int udpdateDeliveredDenomination(AccountId accountId,
                                   String streamPaymentId,
                                   String deliveredAssetCode,
                                   short deliveredAssetScale);
}
