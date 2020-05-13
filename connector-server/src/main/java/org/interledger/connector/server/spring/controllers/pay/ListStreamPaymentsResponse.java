package org.interledger.connector.server.spring.controllers.pay;

import org.interledger.connector.payments.StreamPayment;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;

/**
 * Response object for API requests to list StreamPayments for an account. Results are paginated and
 * do not contain the complete list of payments if the total number of payments is greater than the
 * {@link #pageSize()}
 */
@Value.Immutable
@JsonDeserialize(as = ImmutableListStreamPaymentsResponse.class)
@JsonSerialize(as = ImmutableListStreamPaymentsResponse.class)
public interface ListStreamPaymentsResponse {

  static ImmutableListStreamPaymentsResponse.Builder builder() {
    return ImmutableListStreamPaymentsResponse.builder();
  }

  List<StreamPayment> payments();

  /**
   * Page number (0-based index) for the result list
   * @return
   */
  int pageNumber();

  /**
   * page size used for result pagination. If {@code payments.size() == pageSize()} then more results may exist
   * and client should request more results for next pageNumber.
   * @return
   */
  int pageSize();

}
