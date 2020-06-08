package org.interleger.openpayments.client;

import org.interledger.connector.jackson.ObjectMapperFactory;
import org.interledger.openpayments.Charge;
import org.interledger.openpayments.IlpPaymentDetails;
import org.interledger.openpayments.Invoice;
import org.interledger.openpayments.Mandate;
import org.interledger.openpayments.NewCharge;
import org.interledger.openpayments.NewInvoice;
import org.interledger.openpayments.NewMandate;
import org.interledger.openpayments.OpenPaymentsMediaType;

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
    ACCEPT + OpenPaymentsMediaType.APPLICATION_CONNECTION_JSON_VALUE,
    CONTENT_TYPE + APPLICATION_JSON
  })
  IlpPaymentDetails getIlpInvoicePaymentDetails(
    URI invoiceUrl
  ) throws ThrowableProblem;


  @RequestLine("POST /")
  @Headers({
    ACCEPT + APPLICATION_JSON,
    CONTENT_TYPE + APPLICATION_JSON
  })
  Mandate createMandate(
    URI mandateEndpoint,
    NewMandate newMandate
  );

  @RequestLine("POST /")
  @Headers({
    ACCEPT + APPLICATION_JSON,
    CONTENT_TYPE + APPLICATION_JSON
  })
  Charge createCharge(
    URI mandateUrl,
    NewCharge newCharge
  );
}
