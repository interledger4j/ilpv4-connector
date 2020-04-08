package org.interledger.connector.opa.service.ilp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.interledger.connector.opa.model.problems.InvalidInvoiceSubjectProblem;
import org.interledger.connector.settings.ConnectorSettings;
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
  private ConnectorSettings connectorSettingsMock;

  private String opaUrlPath;

  @Mock
  private PaymentPointerResolver paymentPointerResolver;

  private IlpInvoiceService ilpInvoiceService;

  @Before
  public void setUp() {
    initMocks(this);

    opaUrlPath = "/p/";
    ilpInvoiceService = new IlpInvoiceService(
      () -> connectorSettingsMock,
      paymentPointerResolver,
      opaUrlPath
    );
  }

  ////////////////////
  // cleanupSpspUrlPath
  ////////////////////

  @Test
  public void cleanupSpspUrlPathWithNullBlankEmpty() {
    assertThat(ilpInvoiceService.cleanupOpaUrlPath(null)).isEmpty();
    assertThat(ilpInvoiceService.cleanupOpaUrlPath("")).isEmpty();
    assertThat(ilpInvoiceService.cleanupOpaUrlPath(" ")).isEmpty();
    assertThat(ilpInvoiceService.cleanupOpaUrlPath("/")).isEmpty();
  }

  @Test
  public void cleanupOpaUrlPathWithVariants() {
    assertThat(ilpInvoiceService.cleanupOpaUrlPath("p")).get().isEqualTo("/p");
    assertThat(ilpInvoiceService.cleanupOpaUrlPath("/p")).get().isEqualTo("/p");
    assertThat(ilpInvoiceService.cleanupOpaUrlPath("p/")).get().isEqualTo("/p");
    assertThat(ilpInvoiceService.cleanupOpaUrlPath("/p/")).get().isEqualTo("/p");

    assertThat(ilpInvoiceService.cleanupOpaUrlPath("/p/foo/")).get().isEqualTo("/p/foo");
    assertThat(ilpInvoiceService.cleanupOpaUrlPath("p/foo")).get().isEqualTo("/p/foo");
    assertThat(ilpInvoiceService.cleanupOpaUrlPath("/p/foo/")).get().isEqualTo("/p/foo");
  }

  @Test
  public void computePaymentTargetIntermediatePrefixWithNull() {
    expectedException.expect(NullPointerException.class);
    ilpInvoiceService.computePaymentTargetIntermediatePrefix(null);
  }

  /**
   * When the OPA URL is `/opa`
   */
  @Test
  public void computePaymentTargetIntermediatePrefix() {
    ilpInvoiceService = new IlpInvoiceService(() -> connectorSettingsMock, paymentPointerResolver, "/opa");
    assertThat(ilpInvoiceService.computePaymentTargetIntermediatePrefix("")).isEqualTo("");
    assertThat(ilpInvoiceService.computePaymentTargetIntermediatePrefix(" ")).isEqualTo("");
    assertThat(ilpInvoiceService.computePaymentTargetIntermediatePrefix("/")).isEqualTo("");
    assertThat(ilpInvoiceService.computePaymentTargetIntermediatePrefix("//")).isEqualTo("");
    assertThat(ilpInvoiceService.computePaymentTargetIntermediatePrefix("/  ")).isEqualTo("");
    assertThat(ilpInvoiceService.computePaymentTargetIntermediatePrefix("/foo")).isEqualTo("");
    assertThat(ilpInvoiceService.computePaymentTargetIntermediatePrefix("/foo/")).isEqualTo("");
    assertThat(ilpInvoiceService.computePaymentTargetIntermediatePrefix("/foo.bar")).isEqualTo("");
    assertThat(ilpInvoiceService.computePaymentTargetIntermediatePrefix("/foo.bar/")).isEqualTo("");
    assertThat(ilpInvoiceService.computePaymentTargetIntermediatePrefix("/foo/bar")).isEqualTo("");
    assertThat(ilpInvoiceService.computePaymentTargetIntermediatePrefix("/foo.bar/baz")).isEqualTo("");
    assertThat(ilpInvoiceService.computePaymentTargetIntermediatePrefix("/foo.bar/baz/")).isEqualTo("");
    assertThat(ilpInvoiceService.computePaymentTargetIntermediatePrefix("/opa")).isEqualTo("");
    assertThat(ilpInvoiceService.computePaymentTargetIntermediatePrefix("/opa/")).isEqualTo("");
    assertThat(ilpInvoiceService.computePaymentTargetIntermediatePrefix("/opa//")).isEqualTo("");
    assertThat(ilpInvoiceService.computePaymentTargetIntermediatePrefix("/opa/foo")).isEqualTo("foo");
    assertThat(ilpInvoiceService.computePaymentTargetIntermediatePrefix("/opa/foo/")).isEqualTo("foo");
    assertThat(ilpInvoiceService.computePaymentTargetIntermediatePrefix("/opa/foo//")).isEqualTo("foo");
    assertThat(ilpInvoiceService.computePaymentTargetIntermediatePrefix("/opa/foo.bar")).isEqualTo("foo.bar");
    assertThat(ilpInvoiceService.computePaymentTargetIntermediatePrefix("/opa/foo.bar/")).isEqualTo("foo.bar");
    assertThat(ilpInvoiceService.computePaymentTargetIntermediatePrefix("/opa/foo/bar")).isEqualTo("foo.bar");
    assertThat(ilpInvoiceService.computePaymentTargetIntermediatePrefix("/opa/foo.bar/baz")).isEqualTo("foo.bar.baz");
    assertThat(ilpInvoiceService.computePaymentTargetIntermediatePrefix("/opa/foo.bar/baz/")).isEqualTo("foo.bar.baz");
  }

  @Test
  public void computePaymentTargetIntermediatePrefixWithEmptySpspPath() {
    ilpInvoiceService = new IlpInvoiceService(() -> connectorSettingsMock, paymentPointerResolver, "");
    assertThat(ilpInvoiceService.computePaymentTargetIntermediatePrefix("")).isEqualTo("");
    assertThat(ilpInvoiceService.computePaymentTargetIntermediatePrefix(" ")).isEqualTo("");
    assertThat(ilpInvoiceService.computePaymentTargetIntermediatePrefix("/")).isEqualTo("");
    assertThat(ilpInvoiceService.computePaymentTargetIntermediatePrefix("//")).isEqualTo("");
    assertThat(ilpInvoiceService.computePaymentTargetIntermediatePrefix("/  ")).isEqualTo("");
    assertThat(ilpInvoiceService.computePaymentTargetIntermediatePrefix("/foo")).isEqualTo("foo");
    assertThat(ilpInvoiceService.computePaymentTargetIntermediatePrefix("/foo/")).isEqualTo("foo");
    assertThat(ilpInvoiceService.computePaymentTargetIntermediatePrefix("/foo.bar")).isEqualTo("foo.bar");
    assertThat(ilpInvoiceService.computePaymentTargetIntermediatePrefix("/foo.bar/")).isEqualTo("foo.bar");
    assertThat(ilpInvoiceService.computePaymentTargetIntermediatePrefix("/foo/bar")).isEqualTo("foo.bar");
    assertThat(ilpInvoiceService.computePaymentTargetIntermediatePrefix("/foo.bar/baz")).isEqualTo("foo.bar.baz");
    assertThat(ilpInvoiceService.computePaymentTargetIntermediatePrefix("/foo.bar/baz/")).isEqualTo("foo.bar.baz");
    assertThat(ilpInvoiceService.computePaymentTargetIntermediatePrefix("/p")).isEqualTo("p");
    assertThat(ilpInvoiceService.computePaymentTargetIntermediatePrefix("/p/")).isEqualTo("p");
    assertThat(ilpInvoiceService.computePaymentTargetIntermediatePrefix("/p//")).isEqualTo("p");
    assertThat(ilpInvoiceService.computePaymentTargetIntermediatePrefix("/p/foo")).isEqualTo("p.foo");
    assertThat(ilpInvoiceService.computePaymentTargetIntermediatePrefix("/p/foo/")).isEqualTo("p.foo");
    assertThat(ilpInvoiceService.computePaymentTargetIntermediatePrefix("/p/foo//")).isEqualTo("p.foo");
    assertThat(ilpInvoiceService.computePaymentTargetIntermediatePrefix("/p/foo.bar")).isEqualTo("p.foo.bar");
    assertThat(ilpInvoiceService.computePaymentTargetIntermediatePrefix("/p/foo.bar/")).isEqualTo("p.foo.bar");
    assertThat(ilpInvoiceService.computePaymentTargetIntermediatePrefix("/p/foo/bar")).isEqualTo("p.foo.bar");
    assertThat(ilpInvoiceService.computePaymentTargetIntermediatePrefix("/p/foo.bar/baz")).isEqualTo("p.foo.bar.baz");
    assertThat(ilpInvoiceService.computePaymentTargetIntermediatePrefix("/p/foo.bar/baz/")).isEqualTo("p.foo.bar.baz");
  }

  @Test
  public void getAddressFromInvoiceSubject() {
    String subjectPaymentPointer = "$xpring.money/foo";
    HttpUrl resolvedPaymentPointer = HttpUrl.parse("https://xpring.money/p/foo");

    InterledgerAddress operatorAddress = InterledgerAddress.of("test.jc1");
    when(connectorSettingsMock.operatorAddress()).thenReturn(operatorAddress);
    when(paymentPointerResolver.resolveHttpUrl(eq(PaymentPointer.of(subjectPaymentPointer)))).thenReturn(resolvedPaymentPointer);

    String resolvedAddress = ilpInvoiceService.getAddressFromInvoiceSubject(subjectPaymentPointer);
    assertThat(resolvedAddress).isEqualTo("test.jc1.foo");
  }

  @Test
  public void getAddressFromInvoiceSubjectWithNoOpaPath() {
    ilpInvoiceService = new IlpInvoiceService(
      () -> connectorSettingsMock,
      paymentPointerResolver,
      ""
    );

    String subjectPaymentPointer = "$xpring.money/foo";
    HttpUrl resolvedPaymentPointer = HttpUrl.parse("https://xpring.money/foo");

    InterledgerAddress operatorAddress = InterledgerAddress.of("test.jc1");
    when(connectorSettingsMock.operatorAddress()).thenReturn(operatorAddress);
    when(paymentPointerResolver.resolveHttpUrl(eq(PaymentPointer.of(subjectPaymentPointer)))).thenReturn(resolvedPaymentPointer);

    String resolvedAddress = ilpInvoiceService.getAddressFromInvoiceSubject(subjectPaymentPointer);
    assertThat(resolvedAddress).isEqualTo("test.jc1.foo");
  }

  @Test
  public void getAddressFromInvalidInvoiceSubject() {
    String subjectPaymentPointer = "$xpring.money/foo";
    HttpUrl resolvedPaymentPointer = HttpUrl.parse("https://xpring.money/");

    InterledgerAddress operatorAddress = InterledgerAddress.of("test.jc1");
    when(connectorSettingsMock.operatorAddress()).thenReturn(operatorAddress);
    when(paymentPointerResolver.resolveHttpUrl(eq(PaymentPointer.of(subjectPaymentPointer)))).thenReturn(resolvedPaymentPointer);

    expectedException.expect(InvalidInvoiceSubjectProblem.class);
    ilpInvoiceService.getAddressFromInvoiceSubject(subjectPaymentPointer);
  }
}
