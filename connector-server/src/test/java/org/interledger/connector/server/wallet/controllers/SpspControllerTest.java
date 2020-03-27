package org.interledger.connector.server.wallet.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.stream.receiver.StreamReceiver;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link SpspController}.
 */
public class SpspControllerTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private SpspController spspController;

  @Mock
  private ConnectorSettings connectorSettingsMock;

  @Mock
  private StreamReceiver streamReceiverMock;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    spspController = new SpspController(() -> connectorSettingsMock, streamReceiverMock, "/p");
  }

  ////////////////////
  // cleanupSpspUrlPath
  ////////////////////

  @Test
  public void cleanupSpspUrlPathWithNullBlankEmpty() {
    assertThat(spspController.cleanupSpspUrlPath(null)).isEmpty();
    assertThat(spspController.cleanupSpspUrlPath("")).isEmpty();
    assertThat(spspController.cleanupSpspUrlPath(" ")).isEmpty();
    assertThat(spspController.cleanupSpspUrlPath("/")).isEmpty();
  }

  @Test
  public void cleanupSpspUrlPathWithVariants() {
    assertThat(spspController.cleanupSpspUrlPath("p")).get().isEqualTo("/p");
    assertThat(spspController.cleanupSpspUrlPath("/p")).get().isEqualTo("/p");
    assertThat(spspController.cleanupSpspUrlPath("p/")).get().isEqualTo("/p");
    assertThat(spspController.cleanupSpspUrlPath("/p/")).get().isEqualTo("/p");

    assertThat(spspController.cleanupSpspUrlPath("/p/foo/")).get().isEqualTo("/p/foo");
    assertThat(spspController.cleanupSpspUrlPath("p/foo")).get().isEqualTo("/p/foo");
    assertThat(spspController.cleanupSpspUrlPath("/p/foo/")).get().isEqualTo("/p/foo");
  }

  ////////////////////
  // computePaymentTargetIntermediatePrefix
  ////////////////////

  @Test
  public void computePaymentTargetIntermediatePrefixWithNull() {
    expectedException.expect(NullPointerException.class);
    spspController.computePaymentTargetIntermediatePrefix(null);
  }

  /**
   * When the SPSP URL is `/p`
   */
  @Test
  public void computePaymentTargetIntermediatePrefix() {
    spspController = new SpspController(() -> connectorSettingsMock, streamReceiverMock, "/spsp");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("")).isEqualTo("");
    assertThat(spspController.computePaymentTargetIntermediatePrefix(" ")).isEqualTo("");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/")).isEqualTo("");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("//")).isEqualTo("");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/  ")).isEqualTo("");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/foo")).isEqualTo("");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/foo/")).isEqualTo("");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/foo.bar")).isEqualTo("");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/foo.bar/")).isEqualTo("");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/foo/bar")).isEqualTo("");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/foo.bar/baz")).isEqualTo("");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/foo.bar/baz/")).isEqualTo("");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/spsp")).isEqualTo("");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/spsp/")).isEqualTo("");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/spsp//")).isEqualTo("");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/spsp/foo")).isEqualTo("foo");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/spsp/foo/")).isEqualTo("foo");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/spsp/foo//")).isEqualTo("foo");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/spsp/foo.bar")).isEqualTo("foo.bar");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/spsp/foo.bar/")).isEqualTo("foo.bar");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/spsp/foo/bar")).isEqualTo("foo.bar");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/spsp/foo.bar/baz")).isEqualTo("foo.bar.baz");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/spsp/foo.bar/baz/")).isEqualTo("foo.bar.baz");
  }

  /**
   * When the SPSP URL is ``
   */
  @Test
  public void computePaymentTargetIntermediatePrefixWithOtherPath() {
    spspController = new SpspController(() -> connectorSettingsMock, streamReceiverMock, "/p");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("")).isEqualTo("");
    assertThat(spspController.computePaymentTargetIntermediatePrefix(" ")).isEqualTo("");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/")).isEqualTo("");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("//")).isEqualTo("");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/  ")).isEqualTo("");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/foo")).isEqualTo("");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/foo/")).isEqualTo("");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/foo.bar")).isEqualTo("");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/foo.bar/")).isEqualTo("");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/foo/bar")).isEqualTo("");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/foo.bar/baz")).isEqualTo("");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/foo.bar/baz/")).isEqualTo("");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/p")).isEqualTo("");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/p/")).isEqualTo("");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/p//")).isEqualTo("");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/p/foo")).isEqualTo("foo");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/p/foo/")).isEqualTo("foo");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/p/foo//")).isEqualTo("foo");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/p/foo.bar")).isEqualTo("foo.bar");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/p/foo.bar/")).isEqualTo("foo.bar");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/p/foo/bar")).isEqualTo("foo.bar");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/p/foo.bar/baz")).isEqualTo("foo.bar.baz");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/p/foo.bar/baz/")).isEqualTo("foo.bar.baz");
  }

  @Test
  public void computePaymentTargetIntermediatePrefixWithEmptySpspPath() {
    spspController = new SpspController(() -> connectorSettingsMock, streamReceiverMock, "");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("")).isEqualTo("");
    assertThat(spspController.computePaymentTargetIntermediatePrefix(" ")).isEqualTo("");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/")).isEqualTo("");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("//")).isEqualTo("");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/  ")).isEqualTo("");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/foo")).isEqualTo("foo");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/foo/")).isEqualTo("foo");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/foo.bar")).isEqualTo("foo.bar");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/foo.bar/")).isEqualTo("foo.bar");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/foo/bar")).isEqualTo("foo.bar");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/foo.bar/baz")).isEqualTo("foo.bar.baz");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/foo.bar/baz/")).isEqualTo("foo.bar.baz");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/p")).isEqualTo("p");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/p/")).isEqualTo("p");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/p//")).isEqualTo("p");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/p/foo")).isEqualTo("p.foo");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/p/foo/")).isEqualTo("p.foo");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/p/foo//")).isEqualTo("p.foo");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/p/foo.bar")).isEqualTo("p.foo.bar");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/p/foo.bar/")).isEqualTo("p.foo.bar");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/p/foo/bar")).isEqualTo("p.foo.bar");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/p/foo.bar/baz")).isEqualTo("p.foo.bar.baz");
    assertThat(spspController.computePaymentTargetIntermediatePrefix("/p/foo.bar/baz/")).isEqualTo("p.foo.bar.baz");
  }

}
