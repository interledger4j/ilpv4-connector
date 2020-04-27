package org.interledger.connector.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.interledger.connector.opa.PaymentDetailsService;

import io.xpring.payid.PayID;
import io.xpring.payid.PayIDClient;
import io.xpring.payid.PayIDException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class XrpPaymentDetailsServiceTest {

  @Mock
  private PayIDClient payIDClient;

  private PaymentDetailsService xrpPaymentDetailsService;

  @Before
  public void setUp() {
    initMocks(this);

    xrpPaymentDetailsService = new XrpPaymentDetailsService(payIDClient);
  }

  @Test
  public void getXrpAddressFromValidPayIdSubject() throws PayIDException {
    PayID payID = PayID.builder()
      .host("example.com")
      .account("foo")
      .build();

    String expectedAddress = "ADSFKJANDFAKJNCDAKLSJNCASLKJCN";
    when(payIDClient.xrpAddressForPayID(eq(payID.toString()))).thenReturn(expectedAddress);

    String address = xrpPaymentDetailsService.getAddressFromInvoiceSubject(payID.toString());
    assertThat(address).isEqualTo(expectedAddress);
  }
}
