package org.interledger.connector.gcp;

import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;

import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Value.Immutable
public interface GcpFulfillmentEvent {

  static ImmutableGcpFulfillmentEvent.Builder builder() {
    return ImmutableGcpFulfillmentEvent.builder();
  }

  AccountId prevHopAccount();

  String prevHopAssetCode();

  UnsignedLong prevHopAmount();

  AccountId nextHopAccount();

  String nextHopAssetCode();

  UnsignedLong nextHopAmount();

  @Value.Default
  default BigDecimal spread() {
    return BigDecimal.ZERO;
  }

  BigDecimal exchangeRate();

  Optional<String> senderPaymentPointer();

  Optional<String> destinationPaymentPointer();

  InterledgerAddress connectorIlpAddress();

  InterledgerAddress destinationIlpAddress();

  Optional<InterledgerCondition> fulfillment();

  Instant timestamp();

  short prevHopAssetScale();

  short nextHopAssetScale();

}
