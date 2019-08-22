package com.sappenin.interledger.ilpv4.connector.settlement;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import org.interledger.ilpv4.connector.core.settlement.SettlementQuantity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.util.UUID;

/**
 * Contains an Http response that will be persisted into a cache (e.g., Redis). The primary use-case for this datatype
 * is to determine if a remote API call has already been processed.
 *
 * @deprecated Remove this as part of the "remove request cache" todo.
 */
@Deprecated
@Value.Immutable
@JsonSerialize(as = ImmutableHttpResponseInfo.class)
@JsonDeserialize(as = ImmutableHttpResponseInfo.class)
public interface HttpResponseInfo {

  static ImmutableHttpResponseInfo.Builder builder() {
    return ImmutableHttpResponseInfo.builder();
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
  SettlementQuantity responseBody();

}
