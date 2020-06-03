package org.interledger.connector.wallet;

import static org.interledger.connector.opa.model.OpenPaymentsMediaType.APPLICATION_CONNECTION_JSON_VALUE;

import org.interledger.connector.jackson.ObjectMapperFactory;
import org.interledger.connector.opa.model.IlpPaymentDetails;
import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.NewInvoice;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Feign;
import feign.Headers;
import feign.RequestLine;
import feign.Target;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.optionals.OptionalDecoder;
import org.zalando.problem.ThrowableProblem;

import java.net.URI;

/**
 * A Client that can be used by an Open Payments server to proxy requests to other wallets.
 *
 * Unlike {@link OpenPaymentsClient}, this client makes requests to dynamic clients, which allows request proxying
 * for a given payment identifier.
 */
public interface OpenPaymentsProxyClient {
  String ACCEPT = "Accept:";
  String CONTENT_TYPE = "Content-Type:";
  String AUTHORIZATION = "Authorization:";

  String ID = "id";
  String PREFIX = "prefix";
  String APPLICATION_JSON = "application/json";
  String PLAIN_TEXT = "text/plain";

  /**
   * Static constructor to build a new instance of this Open Payments Client.
   *
   * @return A {@link OpenPaymentsClient}.
   */
  static OpenPaymentsProxyClient construct() {

    final ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapperForProblemsJson();
    return Feign.builder()
      .encoder(new JacksonEncoder(objectMapper))
      .decode404()
      .decoder(new OptionalDecoder(new JacksonDecoder(objectMapper)))
      .target(Target.HardCodedTarget.EmptyTarget.create(OpenPaymentsProxyClient.class));
  }

  @RequestLine("POST /")
  @Headers({
    ACCEPT + APPLICATION_JSON,
    CONTENT_TYPE + APPLICATION_JSON
  })
  Invoice createInvoice(
    URI invoiceEndpoint,
    NewInvoice newInvoice
  ) throws ThrowableProblem;

  @RequestLine("GET /")
  @Headers({
    ACCEPT + APPLICATION_JSON,
    CONTENT_TYPE + APPLICATION_JSON
  })
  Invoice getInvoice(
    URI invoiceUrl
  ) throws ThrowableProblem;

  @RequestLine("GET /")
  @Headers({
    ACCEPT + APPLICATION_CONNECTION_JSON_VALUE,
    CONTENT_TYPE + APPLICATION_JSON
  })
  IlpPaymentDetails getIlpInvoicePaymentDetails(
    URI invoiceUrl
  ) throws ThrowableProblem;
}
