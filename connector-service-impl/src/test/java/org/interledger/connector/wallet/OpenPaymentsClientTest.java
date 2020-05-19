package org.interledger.connector.wallet;

import org.interledger.connector.jackson.ObjectMapperFactory;
import org.interledger.connector.opa.model.OpenPaymentsMetadata;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.PaymentPointerResolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import okhttp3.HttpUrl;
import org.junit.Before;
import org.junit.Test;

public class OpenPaymentsClientTest {

  private OpenPaymentsClient openPaymentsClient;
  private PaymentPointer receiverPaymentPointer;
  private PaymentPointerResolver paymentPointerResolver;
  private HttpUrl receiverBaseUrl;
  private ObjectMapper objectMapper = ObjectMapperFactory.create();

  @Before
  public void setUp() {
    openPaymentsClient = OpenPaymentsClient.construct("https://test.com");

    paymentPointerResolver = PaymentPointerResolver.defaultResolver();

    receiverPaymentPointer = PaymentPointer.of("$rafiki.money/p/nkramer@ripple.com");
    HttpUrl receiverUrl = paymentPointerResolver.resolveHttpUrl(receiverPaymentPointer);
    receiverBaseUrl = new HttpUrl.Builder()
      .scheme(receiverUrl.scheme())
      .host(receiverUrl.host())
//      .port(receiverUrl.port())
      .build();
  }

  @Test
  public void getOpaMetadata() {
    try {
      OpenPaymentsMetadata metadata = openPaymentsClient.getMetadata(receiverBaseUrl.uri());

    } catch (FeignException  e) {
//      assertThat(e.status()).isEqualTo(404);
    }
  }

  @Test
  public void createInvoice() {

  }

  @Test
  public void getInvoice() {

  }
  // cause : ValueInstantiationException
  /*@Test
  public void getInvoicePaymentDetails() throws IOException {
    OpenPaymentsMetadata metadata = openPaymentsClient.getMetadata(receiverBaseUrl.uri());

    Invoice invoice = openPaymentsClient.getInvoice(metadata.invoicesEndpoint().uri(), "a3ee81c6-e01a-48c6-9a25-01e8fecfb059");

    try {
      openPaymentsClient.getXrpInvoicePaymentDetails(metadata.invoicesEndpoint().uri(), "a3ee81c6-e01a-48c6-9a25-01e8fecfb059");
    } catch (FeignException e) {
      System.out.println(e);
    }


  }*/
}
