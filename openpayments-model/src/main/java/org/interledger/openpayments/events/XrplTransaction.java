package org.interledger.openpayments.events;

import org.interledger.connector.opa.model.CorrelationId;
import org.interledger.connector.opa.model.Denomination;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.primitives.UnsignedLong;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableXrplTransaction.class)
@JsonDeserialize(as = ImmutableXrplTransaction.class)
public interface XrplTransaction {
  Logger logger = LoggerFactory.getLogger(XrplTransaction.class);

  @JsonProperty("Account")
  String account();

  @JsonProperty("SourceTag")
  @Nullable
  String sourceTag();

  @JsonProperty("Destination")
  String destination();

  @Nullable
  @JsonProperty("DestinationTag")
  Integer destinationTag();

  @JsonProperty("Amount")
  UnsignedLong amount();

  String hash();

  @JsonProperty("Memos")
  @Nullable
  List<MemoWrapper> memos();

  @Value.Derived
  default Optional<CorrelationId> invoiceMemoCorrelationId() {
    return memos().stream()
      .map(MemoWrapper::memo)
      .filter(memo -> memo.memoType() != null && memo.memoType().equals(Hex.encodeHexString("meme".getBytes())))
      .findFirst()
      .map(memo -> {
        try {
          return CorrelationId.of(new String(Hex.decodeHex(memo.memoData())));
        } catch (DecoderException e) {
          logger.warn("Encountered XRPL transaction with invoice payment memo field, but was unable to decode the MemoData.");
          throw new RuntimeException(e);
        }
      });
  }

  @Value.Default
  default Instant createdAt() {
    return Instant.now();
  };

  @Value.Default
  default Instant modifiedAt() {
    return Instant.now();
  };

  @Value.Default
  default Denomination denomination() {
    return Denomination.builder()
      .assetScale((short) 6)
      .assetCode("XRP")
      .build();
  }

  static ImmutableXrplTransaction.Builder builder() {
    return ImmutableXrplTransaction.builder();
  }

}
