package org.interledger.connector.payid;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@JsonSerialize(as = ImmutablePayIdResponse.class)
@JsonDeserialize(as = ImmutablePayIdResponse.class)
public interface PayIdResponse {

  static ImmutablePayIdResponse.Builder builder() {
    return ImmutablePayIdResponse.builder();
  }

  List<PayIdAddress> addresses();

  String payId();

}
