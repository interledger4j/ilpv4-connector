package org.interledger.connector.transactions;

import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerAddress;

import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value;

import java.time.Instant;

@Value.Immutable
public interface Transaction {

  static ImmutableTransaction.Builder builder() {
    return ImmutableTransaction.builder();
  }

  String referenceId();

  AccountId accountId();

  InterledgerAddress destinationAddress();

  UnsignedLong amount();

  int packetCount();

  String assetCode();

  short assetScale();

  TransactionStatus transactionStatus();

  Instant createdAt();

  Instant modifiedAt();

}
