package org.interledger.connector.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.interledger.connector.opa.PaymentDetailsService;
import org.interledger.connector.opa.model.OpenPaymentsSettings;
import org.interledger.connector.opa.model.problems.InvalidInvoiceSubjectProblem;
import org.interledger.core.InterledgerAddress;
import org.interledger.spsp.PaymentPointerResolver;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

public class IlpPaymentDetailsServiceTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private OpenPaymentsSettings openPaymentsSettingsMock;

  private PaymentPointerResolver paymentPointerResolver;
  private InterledgerAddress operatorAddress;
  private String opaUrlPath;

  private PaymentDetailsService ilpPaymentDetailsService;

  @Before
  public void setUp() throws Exception {
    initMocks(this);

    operatorAddress = InterledgerAddress.of("test.jc1");
    when(openPaymentsSettingsMock.ilpOperatorAddress()).thenReturn(operatorAddress);

    paymentPointerResolver = PaymentPointerResolver.defaultResolver();

    opaUrlPath = "/p/";
    ilpPaymentDetailsService = new IlpPaymentDetailsService(
      () -> openPaymentsSettingsMock,
      opaUrlPath,
      paymentPointerResolver
    );
  }

  @Test
  public void getAddressFromInvoiceSubject() {
    String subjectPaymentPointer = "$xpring.money/p/foo";

    InterledgerAddress operatorAddress = InterledgerAddress.of("test.jc1");
    when(openPaymentsSettingsMock.ilpOperatorAddress()).thenReturn(operatorAddress);

    String resolvedAddress = ilpPaymentDetailsService.getAddressFromInvoiceSubject(subjectPaymentPointer);
    assertThat(resolvedAddress).isEqualTo(operatorAddress.with("foo").getValue());
  }

  @Test
  public void getAddressFromInvoiceSubjectWithNoOpaPath() {
    ilpPaymentDetailsService = new IlpPaymentDetailsService(
      () -> openPaymentsSettingsMock,
      "",
      paymentPointerResolver
    );

    String subjectPaymentPointer = "$xpring.money/foo";

    String resolvedAddress = ilpPaymentDetailsService.getAddressFromInvoiceSubject(subjectPaymentPointer);
    assertThat(resolvedAddress).isEqualTo(operatorAddress.with("foo").getValue());
  }

  @Test
  public void getAddressFromInvalidInvoiceSubject() {
    String subjectPaymentPointer = "$xpring.money";

    InterledgerAddress operatorAddress = InterledgerAddress.of("test.jc1");
    when(openPaymentsSettingsMock.ilpOperatorAddress()).thenReturn(operatorAddress);

    expectedException.expect(InvalidInvoiceSubjectProblem.class);
    ilpPaymentDetailsService.getAddressFromInvoiceSubject(subjectPaymentPointer);
  }
}
