package org.interledger.connector.wallet;

import static org.interledger.connector.opa.model.OpenPaymentsMediaType.APPLICATION_CONNECTION_JSON_VALUE;

import org.interledger.connector.jackson.ObjectMapperFactory;
import org.interledger.connector.opa.model.IlpPaymentDetails;
import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.OpenPaymentsMetadata;
import org.interledger.connector.opa.model.XrpPaymentDetails;
import org.interledger.stream.SendMoneyResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Feign;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import feign.Target;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.optionals.OptionalDecoder;
import okhttp3.HttpUrl;
import org.aspectj.apache.bcel.classfile.Module;
import org.zalando.problem.ThrowableProblem;

import java.net.URI;

public interface OpenPaymentsClient {
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
  static OpenPaymentsClient construct(final String httpUrl) {

    final ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapperForProblemsJson();
    return Feign.builder()
      .encoder(new JacksonEncoder(objectMapper))
      .decode404()
      .decoder(new OptionalDecoder(new JacksonDecoder(objectMapper)))
      .target(OpenPaymentsClient.class, httpUrl);
//      .target(Target.HardCodedTarget.EmptyTarget.create(OpenPaymentsClient.class));
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

  @RequestLine("GET /")
  @Headers({
    ACCEPT + APPLICATION_JSON,
    CONTENT_TYPE + APPLICATION_JSON
  })
  Invoice getInvoice(
    URI invoiceUrl
  ) throws ThrowableProblem;

  @RequestLine("POST {accountId}/invoices/sync?name={invoiceUrl}")
  @Headers({
    ACCEPT + APPLICATION_JSON,
    CONTENT_TYPE + APPLICATION_JSON
  })
  Invoice getOrSyncInvoice(
    @Param("accountId") String accountId,
    @Param("invoiceUrl") String invoiceUrl
  );

  @RequestLine("GET /")
  @Headers({
    ACCEPT + APPLICATION_CONNECTION_JSON_VALUE,
    CONTENT_TYPE + APPLICATION_JSON
  })
  IlpPaymentDetails getIlpInvoicePaymentDetails(
      URI invoiceUrl
  ) throws ThrowableProblem;

  @RequestLine("GET /")
  @Headers({
    ACCEPT + APPLICATION_CONNECTION_JSON_VALUE,
    CONTENT_TYPE + APPLICATION_JSON
  })
  XrpPaymentDetails getXrpInvoicePaymentDetails(
    URI invoiceUrl
  ) throws ThrowableProblem;

  @RequestLine("POST /accounts/{accountId}/invoices/{invoiceId}/pay")
  @Headers({
    ACCEPT + APPLICATION_JSON,
    CONTENT_TYPE + APPLICATION_JSON,
    AUTHORIZATION + "{authorization}"
  })
  SendMoneyResult payInvoice(
    @Param("accountId") String accountId,
    @Param("invoiceId") String invoiceId,
    @Param("authorization") String authorization
  ) throws ThrowableProblem;
}
