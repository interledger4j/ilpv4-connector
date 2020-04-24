package org.interledger.connector.wallet;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.PaymentPointerResolver;

import feign.FeignException;
import okhttp3.HttpUrl;
import org.junit.Before;
import org.junit.Test;

public class OpenPaymentsClientTest {

  private OpenPaymentsClient openPaymentsClient;
  private PaymentPointer receiverPaymentPointer;
  private PaymentPointerResolver paymentPointerResolver;
  private HttpUrl receiverBaseUrl;

  @Before
  public void setUp() {
    openPaymentsClient = OpenPaymentsClient.construct();

    paymentPointerResolver = PaymentPointerResolver.defaultResolver();

    receiverPaymentPointer = PaymentPointer.of("$example.com/foo");
    HttpUrl receiverUrl = paymentPointerResolver.resolveHttpUrl(receiverPaymentPointer);
    receiverBaseUrl = new HttpUrl.Builder()
      .scheme(receiverUrl.scheme())
      .host(receiverUrl.host())
      .port(receiverUrl.port())
      .build();
  }

  @Test
  public void getOpaMetadata() {
    try {
      openPaymentsClient.getMetadata(receiverBaseUrl.toString().replace("/", "").replace("https://", ""));
    } catch (FeignException e) {
      assertThat(e.status()).isEqualTo(404);
    }
  }

  @Test
  public void createInvoice() {

  }

  @Test
  public void getInvoice() {

  }

  @Test
  public void getInvoicePaymentDetails() {

  }
}
