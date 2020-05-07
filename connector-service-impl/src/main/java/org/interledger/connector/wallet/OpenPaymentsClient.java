package org.interledger.connector.wallet;

import org.interledger.connector.jackson.ObjectMapperFactory;
import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.OpenPaymentsMetadata;
import org.interledger.connector.opa.model.XrpPaymentDetails;
import org.interledger.spsp.StreamConnectionDetails;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Feign;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import feign.Response;
import feign.Target;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.optionals.OptionalDecoder;
import org.zalando.problem.ThrowableProblem;

import java.net.URI;

public interface OpenPaymentsClient {
  String ACCEPT = "Accept:";
  String CONTENT_TYPE = "Content-Type:";

  String ID = "id";
  String PREFIX = "prefix";
  String APPLICATION_JSON = "application/json";
  String PLAIN_TEXT = "text/plain";

  /**
   * Static constructor to build a new instance of this Open Payments Client.
   *
   * @return A {@link OpenPaymentsClient}.
   */
  static OpenPaymentsClient construct() {

    final ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapperForProblemsJson();
    return Feign.builder()
      .encoder(new JacksonEncoder(objectMapper))
      .decode404()
      .decoder(new OptionalDecoder(new JacksonDecoder(objectMapper)))
      .target(Target.HardCodedTarget.EmptyTarget.create(OpenPaymentsClient.class));
  }

  @RequestLine("GET /.well-known/open-payments")
  @Headers({
    ACCEPT + APPLICATION_JSON,
    CONTENT_TYPE + APPLICATION_JSON
  })
  OpenPaymentsMetadata getMetadata(URI baseUrl) throws ThrowableProblem;

  @RequestLine("POST /")
  @Headers({
    ACCEPT + APPLICATION_JSON,
    CONTENT_TYPE + APPLICATION_JSON
  })
  Invoice createInvoice(
    URI invoiceEndpoint,
    Invoice invoice
  ) throws ThrowableProblem;

  @RequestLine("GET /{invoiceId}")
  @Headers({
    ACCEPT + APPLICATION_JSON,
    CONTENT_TYPE + APPLICATION_JSON
  })
  Invoice getInvoice(
    URI invoiceUrl
  ) throws ThrowableProblem;

  @RequestLine("OPTIONS /{invoiceId}")
  @Headers({
    ACCEPT + APPLICATION_JSON,
    CONTENT_TYPE + APPLICATION_JSON
  })
  StreamConnectionDetails getIlpInvoicePaymentDetails(
      URI invoiceUrl
  ) throws ThrowableProblem;

  @RequestLine("OPTIONS /")
  @Headers({
    ACCEPT + APPLICATION_JSON,
    CONTENT_TYPE + APPLICATION_JSON
  })
  XrpPaymentDetails getXrpInvoicePaymentDetails(
    URI invoiceUrl
  ) throws ThrowableProblem;
}
