package org.interledger.connector.xumm.model.payload;

import org.interledger.openpayments.xrpl.MemoWrapper;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableTxJson.class)
@JsonDeserialize(as = ImmutableTxJson.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public interface TxJson {

  static ImmutableTxJson.Builder builder() {
    return ImmutableTxJson.builder();
  }

  @JsonProperty("TransactionType")
  String transactionType();

  @JsonProperty("Destination")
  String destination();

  @JsonProperty("DestinationTag")
  Optional<Integer> destinationTag();

  @JsonProperty("Amount")
  @JsonFormat(shape = JsonFormat.Shape.STRING)
  UnsignedLong amount();

  @JsonProperty("Fee")
  @JsonFormat(shape = JsonFormat.Shape.STRING)
  UnsignedLong fee();

  @JsonProperty("Flags")
  Optional<Integer> flags();

  @JsonProperty("Memos")
  @Nullable
  List<MemoWrapper> memos();

}
