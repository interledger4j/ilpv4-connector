package com.sappenin.interledger.ilpv4.connector.settlement;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import org.interledger.ilpv4.connector.core.settlement.Quantity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.util.UUID;

/**
 * Contains data that will be persisted into Redis for determining if a remote API call has already been processed.
 */
@Value.Immutable
@JsonSerialize(as = ImmutableIdempotentResponseInfo.class)
@JsonDeserialize(as = ImmutableIdempotentResponseInfo.class)
public interface IdempotentResponseInfo {

  static ImmutableIdempotentResponseInfo.Builder builder() {
    return ImmutableIdempotentResponseInfo.builder();
  }

  /**
   * The unique identifier of a request.
   *
   * @return
   */
  UUID requestId();

  /**
   * An int representing the Http status for the response.
   *
   * @return
   */
  HttpStatus responseStatus();

  /**
   * The HTTP Headers returned by the response.
   *
   * @return
   */
  HttpHeaders responseHeaders();

  /**
   * @return
   */
  Quantity responseBody();

}
