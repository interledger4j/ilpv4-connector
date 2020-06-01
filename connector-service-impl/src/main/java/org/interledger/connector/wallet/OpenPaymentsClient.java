package org.interledger.connector.wallet;

import static org.interledger.connector.opa.model.OpenPaymentsMediaType.APPLICATION_CONNECTION_JSON_VALUE;

import org.interledger.connector.jackson.ObjectMapperFactory;
import org.interledger.connector.opa.model.Charge;
import org.interledger.connector.opa.model.IlpPaymentDetails;
import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.Mandate;
import org.interledger.connector.opa.model.NewCharge;
import org.interledger.connector.opa.model.NewMandate;
import org.interledger.connector.opa.model.OpenPaymentsMetadata;
import org.interledger.connector.opa.model.PayInvoiceRequest;
import org.interledger.connector.payments.StreamPayment;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Feign;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.optionals.OptionalDecoder;
import org.zalando.problem.ThrowableProblem;

import java.net.URI;
import java.util.Optional;

public interface OpenPaymentsClient {
  String ACCEPT = "Accept:";
  String CONTENT_TYPE = "Content-Type:";
  String AUTHORIZATION = "Authorization:";

  String ID = "id";
  String PREFIX = "prefix";
  String APPLICATION_JSON = "application/json; application/problem+json";
  String PLAIN_TEXT = "text/plain";

  /**
   * Static constructor to build a new instance of this Open Payments Client.
   *
   * @return A {@link OpenPaymentsClient}.
   */
  static OpenPaymentsClient construct(final String httpUrl) {

    final ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapperForProblemsJson();
    return Feign.builder()
      .encoder(new JacksonEncoder(objectMapper))
      .decode404()
      .decoder(new OptionalDecoder(new JacksonDecoder(objectMapper)))
      .target(OpenPaymentsClient.class, httpUrl);
  }

  @RequestLine("GET {accountId}")
  @Headers({
    ACCEPT + APPLICATION_JSON,
    CONTENT_TYPE + APPLICATION_JSON
  })
  OpenPaymentsMetadata getMetadata(URI baseUrl) throws ThrowableProblem;


  @RequestLine("POST accounts/{accountId}/invoices")
  @Headers({
    ACCEPT + APPLICATION_JSON,
    CONTENT_TYPE + APPLICATION_JSON
  })
  Invoice createInvoice(
    @Param("accountId") String accountId,
    Invoice invoice
  ) throws ThrowableProblem;

  @RequestLine("GET accounts/{accountId}/invoices/{invoiceId}")
  @Headers({
    ACCEPT + APPLICATION_JSON,
    CONTENT_TYPE + APPLICATION_JSON
  })
  Invoice getInvoice(
    @Param("accountId") String accountId,
    @Param("invoiceId") String invoiceId
  ) throws ThrowableProblem;

  @RequestLine("POST accounts/{accountId}/invoices/sync?name={invoiceUrl}")
  @Headers({
    ACCEPT + APPLICATION_JSON,
    CONTENT_TYPE + APPLICATION_JSON
  })
  Invoice getOrSyncInvoice(
    @Param("accountId") String accountId,
    @Param("invoiceUrl") String invoiceUrl
  );

  @RequestLine("GET accounts/{accountId}/invoices/{invoiceId}")
  @Headers({
    ACCEPT + APPLICATION_CONNECTION_JSON_VALUE,
    CONTENT_TYPE + APPLICATION_JSON
  })
  IlpPaymentDetails getIlpInvoicePaymentDetails(
    @Param("accountId") String accountId,
    @Param("invoiceId") String invoiceId
  ) throws ThrowableProblem;

  @RequestLine("POST /accounts/{accountId}/invoices/{invoiceId}/pay")
  @Headers({
    ACCEPT + APPLICATION_JSON,
    CONTENT_TYPE + APPLICATION_JSON,
    AUTHORIZATION + "{authorization}"
  })
  StreamPayment payInvoice(
    @Param("accountId") String accountId,
    @Param("invoiceId") String invoiceId,
    @Param("authorization") String authorization,
    Optional<PayInvoiceRequest> payInvoiceRequest
  ) throws ThrowableProblem;

  @RequestLine("POST /accounts/{accountId}/mandates")
  @Headers({
    ACCEPT + APPLICATION_JSON,
    CONTENT_TYPE + APPLICATION_JSON,
    AUTHORIZATION + "{authorization}"
  })
  Mandate createMandate(
    @Param("accountId") String accountId,
    @Param("authorization") String authorization,
    NewMandate newMandate
  ) throws ThrowableProblem;

//  @RequestLine("GET /accounts/{accountId}/mandates")
//  @Headers({
//    ACCEPT + APPLICATION_JSON,
//    CONTENT_TYPE + APPLICATION_JSON,
//    AUTHORIZATION + "{authorization}"
//  })
//  List<Mandate> getMandates(
//    @Param("accountId") String accountId,
//    @Param("authorization") String authorization
//  ) throws ThrowableProblem;
//
  @RequestLine("GET /accounts/{accountId}/mandates/{mandateId}")
  @Headers({
    ACCEPT + APPLICATION_JSON,
    CONTENT_TYPE + APPLICATION_JSON,
    AUTHORIZATION + "{authorization}"
  })
  Mandate findMandateById(
    @Param("accountId") String accountId,
    @Param("mandateId") String mandateId,
    @Param("authorization") String authorization
  ) throws ThrowableProblem;

  @RequestLine("POST /accounts/{accountId}/mandates/{mandateId}/charges")
  @Headers({
    ACCEPT + APPLICATION_JSON,
    CONTENT_TYPE + APPLICATION_JSON,
    AUTHORIZATION + "{authorization}"
  })
  Charge createCharge(
    @Param("accountId") String accountId,
    @Param("mandateId") String mandateId,
    @Param("authorization") String authorization,
    NewCharge newCharge
  ) throws ThrowableProblem;
}
