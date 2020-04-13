package org.interledger.connector.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.interledger.connector.opa.model.OpenPaymentsSettings;
import org.interledger.connector.opa.model.problems.InvalidInvoiceSubjectProblem;
import org.interledger.core.InterledgerAddress;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.PaymentPointerResolver;

import okhttp3.HttpUrl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

public class IlpInvoiceServiceTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private OpenPaymentsSettings openPaymentsSettingsMock;

  private String opaUrlPath;

  @Mock
  private PaymentPointerResolver paymentPointerResolver;

  private IlpInvoiceService ilpInvoiceService;

  @Before
  public void setUp() {
    initMocks(this);

    opaUrlPath = "/p/";
    ilpInvoiceService = new IlpInvoiceService(
      () -> openPaymentsSettingsMock,
      paymentPointerResolver,
      opaUrlPath
    );
  }

  @Test
  public void getAddressFromInvoiceSubject() {
    String subjectPaymentPointer = "$xpring.money/foo";
    HttpUrl resolvedPaymentPointer = HttpUrl.parse("https://xpring.money/p/foo");

    InterledgerAddress operatorAddress = InterledgerAddress.of("test.jc1");
    when(openPaymentsSettingsMock.ilpOperatorAddress()).thenReturn(operatorAddress);
    when(paymentPointerResolver.resolveHttpUrl(eq(PaymentPointer.of(subjectPaymentPointer)))).thenReturn(resolvedPaymentPointer);

    String resolvedAddress = ilpInvoiceService.getAddressFromInvoiceSubject(subjectPaymentPointer);
    assertThat(resolvedAddress).isEqualTo("test.jc1.foo");
  }

  @Test
  public void getAddressFromInvoiceSubjectWithNoOpaPath() {
    ilpInvoiceService = new IlpInvoiceService(
      () -> openPaymentsSettingsMock,
      paymentPointerResolver,
      ""
    );

    String subjectPaymentPointer = "$xpring.money/foo";
    HttpUrl resolvedPaymentPointer = HttpUrl.parse("https://xpring.money/foo");

    InterledgerAddress operatorAddress = InterledgerAddress.of("test.jc1");
    when(openPaymentsSettingsMock.ilpOperatorAddress()).thenReturn(operatorAddress);
    when(paymentPointerResolver.resolveHttpUrl(eq(PaymentPointer.of(subjectPaymentPointer)))).thenReturn(resolvedPaymentPointer);

    String resolvedAddress = ilpInvoiceService.getAddressFromInvoiceSubject(subjectPaymentPointer);
    assertThat(resolvedAddress).isEqualTo("test.jc1.foo");
  }

  @Test
  public void getAddressFromInvalidInvoiceSubject() {
    String subjectPaymentPointer = "$xpring.money/foo";
    HttpUrl resolvedPaymentPointer = HttpUrl.parse("https://xpring.money/");

    InterledgerAddress operatorAddress = InterledgerAddress.of("test.jc1");
    when(openPaymentsSettingsMock.ilpOperatorAddress()).thenReturn(operatorAddress);
    when(paymentPointerResolver.resolveHttpUrl(eq(PaymentPointer.of(subjectPaymentPointer)))).thenReturn(resolvedPaymentPointer);

    expectedException.expect(InvalidInvoiceSubjectProblem.class);
    ilpInvoiceService.getAddressFromInvoiceSubject(subjectPaymentPointer);
  }
}
