package org.interledger.connector.wallet;

import org.interledger.connector.jackson.ObjectMapperFactory;
import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.OpenPaymentsMetadata;
import org.interledger.spsp.StreamConnectionDetails;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Feign;
import feign.Headers;
import feign.Param;
import feign.RequestInterceptor;
import feign.RequestLine;
import feign.Target;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.optionals.OptionalDecoder;
import okhttp3.HttpUrl;
import org.zalando.problem.ThrowableProblem;

import java.util.Objects;

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

  @RequestLine("GET {baseUrl}/.well-known/open-payments")
  @Headers({
    ACCEPT + APPLICATION_JSON,
    CONTENT_TYPE + APPLICATION_JSON
  })
  OpenPaymentsMetadata getMetadata(@Param("baseUrl") String baseUrl) throws ThrowableProblem;

  @RequestLine("POST {invoiceEndpoint}")
  @Headers({
    ACCEPT + APPLICATION_JSON,
    CONTENT_TYPE + APPLICATION_JSON
  })
  Invoice createInvoice(@Param("invoiceEndpoint") String invoiceEndpoint, Invoice invoice) throws ThrowableProblem;

  @RequestLine("GET {invoiceEndpoint}/{invoiceId}")
  @Headers({
    ACCEPT + APPLICATION_JSON,
    CONTENT_TYPE + APPLICATION_JSON
  })
  Invoice getInvoice(
    @Param("invoiceEndpoint") String invoiceEndpoint,
    @Param("invoiceId") String invoiceId
  ) throws ThrowableProblem;

  @RequestLine("OPTIONS {invoiceEndpoint}/{invoiceId}")
  @Headers({
    ACCEPT + APPLICATION_JSON,
    CONTENT_TYPE + APPLICATION_JSON
  })
  StreamConnectionDetails getInvoicePaymentDetails(
    @Param("invoiceEndpoint") String invoiceEndpoint,
    @Param("invoiceId") String invoiceId
  ) throws ThrowableProblem;
}
