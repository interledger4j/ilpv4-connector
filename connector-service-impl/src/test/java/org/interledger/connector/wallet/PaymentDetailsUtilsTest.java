package org.interledger.connector.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Optional;

public class PaymentDetailsUtilsTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Before
  public void setUp() {
    initMocks(this);
  }

  @Test
  public void cleanupSpspUrlPathWithNullBlankEmpty() {
    assertThat(PaymentDetailsUtils.cleanupUrlPath(null)).isEmpty();
    assertThat(PaymentDetailsUtils.cleanupUrlPath("")).isEmpty();
    assertThat(PaymentDetailsUtils.cleanupUrlPath(" ")).isEmpty();
    assertThat(PaymentDetailsUtils.cleanupUrlPath("/")).isEmpty();
  }

  @Test
  public void cleanupUrlPathWithVariants() {
    assertThat(PaymentDetailsUtils.cleanupUrlPath("p")).get().isEqualTo("/p");
    assertThat(PaymentDetailsUtils.cleanupUrlPath("/p")).get().isEqualTo("/p");
    assertThat(PaymentDetailsUtils.cleanupUrlPath("p/")).get().isEqualTo("/p");
    assertThat(PaymentDetailsUtils.cleanupUrlPath("/p/")).get().isEqualTo("/p");

    assertThat(PaymentDetailsUtils.cleanupUrlPath("/p/foo/")).get().isEqualTo("/p/foo");
    assertThat(PaymentDetailsUtils.cleanupUrlPath("p/foo")).get().isEqualTo("/p/foo");
    assertThat(PaymentDetailsUtils.cleanupUrlPath("/p/foo/")).get().isEqualTo("/p/foo");
  }

  @Test
  public void computePaymentTargetIntermediatePrefixWithNull() {
    expectedException.expect(NullPointerException.class);
    PaymentDetailsUtils.computePaymentTargetIntermediatePrefix(null, Optional.empty());
  }

  /**
   * When the OPA URL is `/p`
   */
  @Test
  public void computePaymentTargetIntermediatePrefix() {
    Optional<String> baseUrlPath = Optional.of("/org/interledger/connector/p");
    assertThat(PaymentDetailsUtils.computePaymentTargetIntermediatePrefix("", baseUrlPath)).isEqualTo("");
    assertThat(PaymentDetailsUtils.computePaymentTargetIntermediatePrefix(" ", baseUrlPath)).isEqualTo("");
    assertThat(PaymentDetailsUtils.computePaymentTargetIntermediatePrefix("/", baseUrlPath)).isEqualTo("");
    assertThat(PaymentDetailsUtils.computePaymentTargetIntermediatePrefix("//", baseUrlPath)).isEqualTo("");
    assertThat(PaymentDetailsUtils.computePaymentTargetIntermediatePrefix("/  ", baseUrlPath)).isEqualTo("");
    assertThat(PaymentDetailsUtils.computePaymentTargetIntermediatePrefix("/foo", baseUrlPath)).isEqualTo("");
    assertThat(PaymentDetailsUtils.computePaymentTargetIntermediatePrefix("/foo/", baseUrlPath)).isEqualTo("");
    assertThat(PaymentDetailsUtils.computePaymentTargetIntermediatePrefix("/foo.bar", baseUrlPath)).isEqualTo("");
    assertThat(PaymentDetailsUtils.computePaymentTargetIntermediatePrefix("/foo.bar/", baseUrlPath)).isEqualTo("");
    assertThat(PaymentDetailsUtils.computePaymentTargetIntermediatePrefix("/foo/bar", baseUrlPath)).isEqualTo("");
    assertThat(PaymentDetailsUtils.computePaymentTargetIntermediatePrefix("/foo.bar/baz", baseUrlPath)).isEqualTo("");
    assertThat(PaymentDetailsUtils.computePaymentTargetIntermediatePrefix("/foo.bar/baz/", baseUrlPath)).isEqualTo("");
    assertThat(PaymentDetailsUtils.computePaymentTargetIntermediatePrefix("/org/interledger/connector/p", baseUrlPath)).isEqualTo("");
    assertThat(PaymentDetailsUtils.computePaymentTargetIntermediatePrefix("/org/interledger/connector/p/", baseUrlPath)).isEqualTo("");
    assertThat(PaymentDetailsUtils.computePaymentTargetIntermediatePrefix("/org/interledger/connector/p//", baseUrlPath)).isEqualTo("");
    assertThat(PaymentDetailsUtils.computePaymentTargetIntermediatePrefix("/org/interledger/connector/p/foo", baseUrlPath)).isEqualTo("foo");
    assertThat(PaymentDetailsUtils.computePaymentTargetIntermediatePrefix("/org/interledger/connector/p/foo/", baseUrlPath)).isEqualTo("foo");
    assertThat(PaymentDetailsUtils.computePaymentTargetIntermediatePrefix("/org/interledger/connector/p/foo//", baseUrlPath)).isEqualTo("foo");
    assertThat(PaymentDetailsUtils.computePaymentTargetIntermediatePrefix("/org/interledger/connector/p/foo.bar", baseUrlPath)).isEqualTo("foo.bar");
    assertThat(PaymentDetailsUtils.computePaymentTargetIntermediatePrefix("/org/interledger/connector/p/foo.bar/", baseUrlPath)).isEqualTo("foo.bar");
    assertThat(PaymentDetailsUtils.computePaymentTargetIntermediatePrefix("/org/interledger/connector/p/foo/bar", baseUrlPath)).isEqualTo("foo.bar");
    assertThat(PaymentDetailsUtils.computePaymentTargetIntermediatePrefix("/org/interledger/connector/p/foo.bar/baz", baseUrlPath)).isEqualTo("foo.bar.baz");
    assertThat(PaymentDetailsUtils.computePaymentTargetIntermediatePrefix("/org/interledger/connector/p/foo.bar/baz/", baseUrlPath)).isEqualTo("foo.bar.baz");
  }

  @Test
  public void computePaymentTargetIntermediatePrefixWithEmptyBaseUrlPath() {
    Optional<String> baseUrlPath = Optional.empty();
    assertThat(PaymentDetailsUtils.computePaymentTargetIntermediatePrefix("", baseUrlPath)).isEqualTo("");
    assertThat(PaymentDetailsUtils.computePaymentTargetIntermediatePrefix(" ", baseUrlPath)).isEqualTo("");
    assertThat(PaymentDetailsUtils.computePaymentTargetIntermediatePrefix("/", baseUrlPath)).isEqualTo("");
    assertThat(PaymentDetailsUtils.computePaymentTargetIntermediatePrefix("//", baseUrlPath)).isEqualTo("");
    assertThat(PaymentDetailsUtils.computePaymentTargetIntermediatePrefix("/  ", baseUrlPath)).isEqualTo("");
    assertThat(PaymentDetailsUtils.computePaymentTargetIntermediatePrefix("/foo", baseUrlPath)).isEqualTo("foo");
    assertThat(PaymentDetailsUtils.computePaymentTargetIntermediatePrefix("/foo/", baseUrlPath)).isEqualTo("foo");
    assertThat(PaymentDetailsUtils.computePaymentTargetIntermediatePrefix("/foo.bar", baseUrlPath)).isEqualTo("foo.bar");
    assertThat(PaymentDetailsUtils.computePaymentTargetIntermediatePrefix("/foo.bar/", baseUrlPath)).isEqualTo("foo.bar");
    assertThat(PaymentDetailsUtils.computePaymentTargetIntermediatePrefix("/foo/bar", baseUrlPath)).isEqualTo("foo.bar");
    assertThat(PaymentDetailsUtils.computePaymentTargetIntermediatePrefix("/foo.bar/baz", baseUrlPath)).isEqualTo("foo.bar.baz");
    assertThat(PaymentDetailsUtils.computePaymentTargetIntermediatePrefix("/foo.bar/baz/", baseUrlPath)).isEqualTo("foo.bar.baz");
    assertThat(PaymentDetailsUtils.computePaymentTargetIntermediatePrefix("/p", baseUrlPath)).isEqualTo("p");
    assertThat(PaymentDetailsUtils.computePaymentTargetIntermediatePrefix("/p/", baseUrlPath)).isEqualTo("p");
    assertThat(PaymentDetailsUtils.computePaymentTargetIntermediatePrefix("/p//", baseUrlPath)).isEqualTo("p");
    assertThat(PaymentDetailsUtils.computePaymentTargetIntermediatePrefix("/p/foo", baseUrlPath)).isEqualTo("p.foo");
    assertThat(PaymentDetailsUtils.computePaymentTargetIntermediatePrefix("/p/foo/", baseUrlPath)).isEqualTo("p.foo");
    assertThat(PaymentDetailsUtils.computePaymentTargetIntermediatePrefix("/p/foo//", baseUrlPath)).isEqualTo("p.foo");
    assertThat(PaymentDetailsUtils.computePaymentTargetIntermediatePrefix("/p/foo.bar", baseUrlPath)).isEqualTo("p.foo.bar");
    assertThat(PaymentDetailsUtils.computePaymentTargetIntermediatePrefix("/p/foo.bar/", baseUrlPath)).isEqualTo("p.foo.bar");
    assertThat(PaymentDetailsUtils.computePaymentTargetIntermediatePrefix("/p/foo/bar", baseUrlPath)).isEqualTo("p.foo.bar");
    assertThat(PaymentDetailsUtils.computePaymentTargetIntermediatePrefix("/p/foo.bar/baz", baseUrlPath)).isEqualTo("p.foo.bar.baz");
    assertThat(PaymentDetailsUtils.computePaymentTargetIntermediatePrefix("/p/foo.bar/baz/", baseUrlPath)).isEqualTo("p.foo.bar.baz");
  }

}
