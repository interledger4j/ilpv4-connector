package org.interledger.connector.gcp;

import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value;

import java.math.BigDecimal;
import java.time.Instant;
import javax.annotation.Nullable;

@Value.Immutable
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonSerialize(as=ImmutableGcpPacketResponseEvent.class)
@JsonDeserialize(as=ImmutableGcpPacketResponseEvent.class)
public interface GcpPacketResponseEvent {

  static ImmutableGcpPacketResponseEvent.Builder builder() {
    return ImmutableGcpPacketResponseEvent.builder();
  }

  AccountId prevHopAccount();

  String prevHopAssetCode();

  UnsignedLong prevHopAmount();

  @Nullable
  AccountId nextHopAccount();

  @Nullable
  String nextHopAssetCode();

  @Nullable
  UnsignedLong nextHopAmount();

  @Value.Default
  default BigDecimal spread() {
    return BigDecimal.ZERO;
  }

  @Nullable
  BigDecimal exchangeRate();

  InterledgerAddress connectorIlpAddress();

  InterledgerAddress destinationIlpAddress();

  @Nullable
  InterledgerCondition fulfillment();

  Instant timestamp();

  int prevHopAssetScale();

  @Nullable
  Integer nextHopAssetScale();

  String status();

  @Nullable
  String rejectionMessage();

  @Nullable
  String rejectionCode();

  @Nullable
  String rejectionTriggeredBy();

}
