package org.interledger.connector.transactions;

import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerAddress;

import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value;

import java.time.Instant;
import java.util.Optional;

/**
 *
 */
@Value.Immutable
public interface Transaction {

  static ImmutableTransaction.Builder builder() {
    return ImmutableTransaction.builder();
  }

  /**
   * Reference Id for transaction. Locally unique by accountId.
   * @return
   */
  String referenceId();

  /**
   * AccountId that this transaction is attached to.
   * @return
   */
  AccountId accountId();

  /**
   * Source address that initiated the payment. Optional because in the case of payments received, the source address
   * will not be known if/until the sender sends a ConnectionNewAddress frame (which is not guaranteed).
   * @return
   */
  Optional<InterledgerAddress> sourceAddress();

  /**
   * Destination address where the payment was sent.
   * @return
   */
  InterledgerAddress destinationAddress();

  /**
   * Amount (in assetScale) of the accountId's account settings at the time the transaction was created.
   * @return
   */
  UnsignedLong amount();

  /**
   * Number of ILP packets that were aggregated into this transaction.
   * @return
   */
  int packetCount();

  /**
   * Asset code of the accountId's account settings at the time the transaction was created.
   * @return
   */
  String assetCode();


  /**
   * Asset scale of the accountId's account settings at the time the transaction was created.
   * @return
   */
  short assetScale();

  TransactionStatus status();

  TransactionType type();

  Instant createdAt();

  Instant modifiedAt();

}
