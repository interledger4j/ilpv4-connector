package org.interledger.connector.server.spring.controllers.pay;

import org.interledger.connector.payments.StreamPayment;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@JsonDeserialize(as = ImmutableListPaymentsResponse.class)
@JsonSerialize(as = ImmutableListPaymentsResponse.class)
public interface ListPaymentsResponse {

  static ImmutableListPaymentsResponse.Builder builder() {
    return ImmutableListPaymentsResponse.builder();
  }

  List<StreamPayment> payments();

  int pageNumber();

  int pageSize();

}
