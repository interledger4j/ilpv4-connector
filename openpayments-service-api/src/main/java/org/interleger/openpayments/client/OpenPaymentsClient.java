package org.interleger.openpayments.client;

import org.interledger.connector.jackson.ObjectMapperFactory;
import org.interledger.connector.payments.StreamPayment;
import org.interledger.openpayments.Charge;
import org.interledger.openpayments.IlpPaymentDetails;
import org.interledger.openpayments.Invoice;
import org.interledger.openpayments.Mandate;
import org.interledger.openpayments.NewCharge;
import org.interledger.openpayments.NewInvoice;
import org.interledger.openpayments.NewMandate;
import org.interledger.openpayments.OpenPaymentsMediaType;
import org.interledger.openpayments.PayInvoiceRequest;
import org.interledger.openpayments.config.OpenPaymentsMetadata;
import org.interledger.openpayments.config.OpenPaymentsPathConstants;

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
import java.util.List;
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


  @RequestLine("POST " + OpenPaymentsPathConstants.INVOICES_BASE)
  @Headers({
    ACCEPT + APPLICATION_JSON,
    CONTENT_TYPE + APPLICATION_JSON
  })
  Invoice createInvoice(
    @Param("accountId") String accountId,
    NewInvoice newInvoice
  ) throws ThrowableProblem;

  @RequestLine("GET " + OpenPaymentsPathConstants.INVOICES_WITH_ID)
  @Headers({
    ACCEPT + APPLICATION_JSON,
    CONTENT_TYPE + APPLICATION_JSON
  })
  Invoice getInvoice(
    @Param("accountId") String accountId,
    @Param("invoiceId") String invoiceId
  ) throws ThrowableProblem;

  @RequestLine("POST " + OpenPaymentsPathConstants.SYNC_INVOICE + "?name={invoiceUrl}")
  @Headers({
    ACCEPT + APPLICATION_JSON,
    CONTENT_TYPE + APPLICATION_JSON
  })
  Invoice syncInvoice(
    @Param("accountId") String accountId,
    @Param("invoiceUrl") String invoiceUrl
  );

  @RequestLine("GET " + OpenPaymentsPathConstants.INVOICES_WITH_ID)
  @Headers({
    ACCEPT + OpenPaymentsMediaType.APPLICATION_CONNECTION_JSON_VALUE,
    CONTENT_TYPE + APPLICATION_JSON
  })
  IlpPaymentDetails getIlpInvoicePaymentDetails(
    @Param("accountId") String accountId,
    @Param("invoiceId") String invoiceId
  ) throws ThrowableProblem;

  @RequestLine("POST " + OpenPaymentsPathConstants.PAY_INVOICE)
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

  @RequestLine("GET /accounts/{accountId}/mandates")
  @Headers({
    ACCEPT + APPLICATION_JSON,
    CONTENT_TYPE + APPLICATION_JSON,
    AUTHORIZATION + "{authorization}"
  })
  List<Mandate> getMandates(
    @Param("accountId") String accountId,
    @Param("authorization") String authorization
  ) throws ThrowableProblem;

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
