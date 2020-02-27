package org.interledger.connector.links.filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.link.Link;
import org.interledger.link.LinkSettings;
import org.interledger.link.LoopbackLink;
import org.interledger.link.PacketRejector;

import com.google.api.client.util.Lists;
import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Unit tests for {@link DefaultLinkFilterChain}.
 */
public class DefaultLinkFilterChainTest {

  private static final InterledgerAddress OPERATOR_ADDRESS = InterledgerAddress.of("example.operator");

  // The AccountId of the Outbound Link
  private static final AccountId OUTGOING_ACCOUNT_ID = AccountId.of("destination-account");
  private static final AccountSettings OUTGOING_ACCOUNT_SETTINGS = AccountSettings.builder()
    .accountId(OUTGOING_ACCOUNT_ID)
    .accountRelationship(AccountRelationship.PEER)
    .assetCode("USD")
    .assetScale(9)
    .linkType(LoopbackLink.LINK_TYPE)
    .build();
  private static final LinkSettings OUTGOING_LINK_SETTINGS = LinkSettings.builder()
    .linkType(LoopbackLink.LINK_TYPE)
    .putCustomSettings("accountId", OUTGOING_ACCOUNT_ID.value())
    .build();

  private static final InterledgerPreparePacket PREPARE_PACKET = InterledgerPreparePacket.builder()
    .destination(InterledgerAddress.of("example.foo"))
    .amount(UnsignedLong.ONE)
    .expiresAt(Instant.now().plusSeconds(30))
    .executionCondition(InterledgerCondition.of(new byte[32]))
    .build();

  private AtomicBoolean linkFilter1PreProcessed;
  private AtomicBoolean linkFilter1PostProcessed;
  private AtomicBoolean linkFilter2PreProcessed;
  private AtomicBoolean linkFilter2PostProcessed;
  private AtomicBoolean linkFilter3PreProcessed;
  private AtomicBoolean linkFilter3PostProcessed;

  private List<LinkFilter> linkFilters;
  private Link outgoingLink;

  private DefaultLinkFilterChain filterChain;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    linkFilter1PreProcessed = new AtomicBoolean(false);
    linkFilter2PreProcessed = new AtomicBoolean(false);
    linkFilter3PreProcessed = new AtomicBoolean(false);
    linkFilter1PostProcessed = new AtomicBoolean(false);
    linkFilter2PostProcessed = new AtomicBoolean(false);
    linkFilter3PostProcessed = new AtomicBoolean(false);

    this.outgoingLink = new LoopbackLink(
      () -> OPERATOR_ADDRESS,
      OUTGOING_LINK_SETTINGS,
      new PacketRejector(() -> OPERATOR_ADDRESS)
    );

    this.linkFilters = Lists.newArrayList();
    filterChain = new DefaultLinkFilterChain(
      new PacketRejector(() -> OPERATOR_ADDRESS),
      linkFilters,
      outgoingLink
    );
  }

  @Test
  public void filterPacketWithNoFilters() {
    assertThat(this.linkFilters.size()).isEqualTo(0);

    filterChain.doFilter(OUTGOING_ACCOUNT_SETTINGS, PREPARE_PACKET).handle(
      fulfillPacket -> assertThat(fulfillPacket.getFulfillment()).isEqualTo(LoopbackLink.LOOPBACK_FULFILLMENT),
      rejectPacket -> fail("Should have fulfilled but rejected!")
    );
  }

  @Test
  public void filterPacketWithMultipleFilters() {
    final LinkFilter linkFilter1 = (sourceAccountSettings, sourcePreparePacket, filterChain) ->
    {
      linkFilter1PreProcessed.set(true);
      final InterledgerResponsePacket response = filterChain.doFilter(sourceAccountSettings, sourcePreparePacket);
      linkFilter1PostProcessed.set(true);
      return response;
    };
    this.linkFilters.add(linkFilter1);

    final LinkFilter linkFilter2 = (sourceAccountSettings, sourcePreparePacket, filterChain) ->
    {
      linkFilter2PreProcessed.set(true);
      final InterledgerResponsePacket response = filterChain.doFilter(sourceAccountSettings, sourcePreparePacket);
      linkFilter2PostProcessed.set(true);
      return response;
    };
    this.linkFilters.add(linkFilter2);

    assertThat(this.linkFilters.size()).isEqualTo(2);

    filterChain.doFilter(OUTGOING_ACCOUNT_SETTINGS, PREPARE_PACKET).handle(
      fulfillPacket -> assertThat(fulfillPacket.getFulfillment()).isEqualTo(LoopbackLink.LOOPBACK_FULFILLMENT),
      rejectPacket -> fail("Should have fulfilled but rejected!")
    );

    // Exception in second filter means 1 and 2 get called, but not 3.
    assertThat(linkFilter1PreProcessed).isTrue();
    assertThat(linkFilter1PostProcessed).isTrue();
    assertThat(linkFilter2PreProcessed).isTrue();
    assertThat(linkFilter2PostProcessed).isTrue();
    assertThat(linkFilter3PreProcessed).isFalse();
    assertThat(linkFilter3PostProcessed).isFalse();
  }


  /**
   * In this test, an exception is thrown in the first filter. The test verifies that only filters 1 is processed, but
   * filter 2, 3, and the rest of the filter-chain are un-processed.
   */
  @Test
  public void filterPacketWithExceptionInFirstFilter() {
    final LinkFilter linkFilter1 = (sourceAccountSettings, sourcePreparePacket, filterChain) -> {
      linkFilter1PreProcessed.set(true);
      throw new RuntimeException("Simulated LinkFilter exception");
    };
    this.linkFilters.add(linkFilter1);

    final LinkFilter linkFilter2 = (sourceAccountSettings, sourcePreparePacket, filterChain) -> {
      linkFilter2PreProcessed.set(true);
      InterledgerResponsePacket responsePacket = filterChain.doFilter(sourceAccountSettings, sourcePreparePacket);
      linkFilter2PostProcessed.set(true);
      return responsePacket;
    };
    this.linkFilters.add(linkFilter2);

    final LinkFilter linkFilter3 = (sourceAccountSettings, sourcePreparePacket, filterChain) -> {
      linkFilter3PreProcessed.set(true);
      InterledgerResponsePacket responsePacket = filterChain.doFilter(sourceAccountSettings, sourcePreparePacket);
      linkFilter3PostProcessed.set(true);
      return responsePacket;
    };
    this.linkFilters.add(linkFilter3);

    assertThat(this.linkFilters.size()).isEqualTo(3);

    filterChain.doFilter(OUTGOING_ACCOUNT_SETTINGS, PREPARE_PACKET).handle(
      fulfillPacket -> fail("Should have rejected but fulfilled!"),
      rejectPacket -> assertThat(rejectPacket.getCode()).isEqualTo(InterledgerErrorCode.T00_INTERNAL_ERROR)
    );

    // Exception in first filter means 2 and 3 don't get called.
    assertThat(linkFilter1PreProcessed).isTrue();
    assertThat(linkFilter1PostProcessed).isFalse();
    assertThat(linkFilter2PreProcessed).isFalse();
    assertThat(linkFilter2PostProcessed).isFalse();
    assertThat(linkFilter3PreProcessed).isFalse();
    assertThat(linkFilter3PostProcessed).isFalse();
  }

  /**
   * In this test, an exception is thrown in the second filter. The test verifies that only filter 1 and 2 are
   * processed, but filter 3 and the rest of the filter-chain are un-processed.
   */
  @Test
  public void filterPacketWithExceptionInMiddleFilter() {
    final LinkFilter linkFilter1 = (sourceAccountSettings, sourcePreparePacket, filterChain) -> {
      linkFilter1PreProcessed.set(true);
      InterledgerResponsePacket responsePacket = filterChain.doFilter(sourceAccountSettings, sourcePreparePacket);
      linkFilter1PostProcessed.set(true);
      return responsePacket;
    };
    this.linkFilters.add(linkFilter1);

    final LinkFilter linkFilter2 = (sourceAccountSettings, sourcePreparePacket, filterChain) -> {
      linkFilter2PreProcessed.set(true);
      throw new RuntimeException("Simulated LinkFilter exception");
    };
    this.linkFilters.add(linkFilter2);

    final LinkFilter linkFilter3 = (sourceAccountSettings, sourcePreparePacket, filterChain) -> {
      linkFilter3PreProcessed.set(true);
      InterledgerResponsePacket responsePacket = filterChain.doFilter(sourceAccountSettings, sourcePreparePacket);
      linkFilter3PostProcessed.set(true);
      return responsePacket;
    };
    this.linkFilters.add(linkFilter3);

    assertThat(this.linkFilters.size()).isEqualTo(3);

    filterChain.doFilter(OUTGOING_ACCOUNT_SETTINGS, PREPARE_PACKET).handle(
      fulfillPacket -> fail("Should have rejected but fulfilled!"),
      rejectPacket -> assertThat(rejectPacket.getCode()).isEqualTo(InterledgerErrorCode.T00_INTERNAL_ERROR)
    );

    // Exception in second filter means 1 and 2 get called, but not 3.
    assertThat(linkFilter1PreProcessed).isTrue();
    assertThat(linkFilter1PostProcessed).isTrue();
    assertThat(linkFilter2PreProcessed).isTrue();
    assertThat(linkFilter2PostProcessed).isFalse();
    assertThat(linkFilter3PreProcessed).isFalse();
    assertThat(linkFilter3PostProcessed).isFalse();
  }

  /**
   * In this test, an exception is thrown in the last filter. The test verifies that filters 1 and 2 are still
   * processed.
   */
  @Test
  public void filterPacketWithExceptionInLastFilter() {
    final LinkFilter linkFilter1 = (sourceAccountSettings, sourcePreparePacket, filterChain) -> {
      linkFilter1PreProcessed.set(true);
      InterledgerResponsePacket responsePacket = filterChain.doFilter(sourceAccountSettings, sourcePreparePacket);
      linkFilter1PostProcessed.set(true);
      return responsePacket;
    };
    this.linkFilters.add(linkFilter1);

    final LinkFilter linkFilter2 = (sourceAccountSettings, sourcePreparePacket, filterChain) -> {
      linkFilter2PreProcessed.set(true);
      InterledgerResponsePacket responsePacket = filterChain.doFilter(sourceAccountSettings, sourcePreparePacket);
      linkFilter2PostProcessed.set(true);
      return responsePacket;
    };
    this.linkFilters.add(linkFilter2);

    final LinkFilter linkFilter3 = (sourceAccountSettings, sourcePreparePacket, filterChain) -> {
      linkFilter3PreProcessed.set(true);
      throw new RuntimeException("Simulated LinkFilter exception");
    };
    this.linkFilters.add(linkFilter3);

    assertThat(this.linkFilters.size()).isEqualTo(3);

    filterChain.doFilter(OUTGOING_ACCOUNT_SETTINGS, PREPARE_PACKET).handle(
      fulfillPacket -> fail("Should have rejected but fulfilled!"),
      rejectPacket -> assertThat(rejectPacket.getCode()).isEqualTo(InterledgerErrorCode.T00_INTERNAL_ERROR)
    );

    // Exception in last filter means 1 and 2 get called, but only half of 3.
    assertThat(linkFilter1PreProcessed).isTrue();
    assertThat(linkFilter1PostProcessed).isTrue();
    assertThat(linkFilter2PreProcessed).isTrue();
    assertThat(linkFilter2PostProcessed).isTrue();
    assertThat(linkFilter3PreProcessed).isTrue();
    assertThat(linkFilter3PostProcessed).isFalse();
  }

  /**
   * In this test, an exception is thrown in the final portion of the Filter-chain (after all of the filters have been
   * processed) due to an expired packet. This test verifies that all filters are processed on the return-path.
   *
   * @see "https://github.com/interledger4j/ilpv4-connector/issues/593"
   * @see "https://github.com/interledger4j/ilpv4-connector/issues/588"
   */
  @Test
  public void filterPacketWithExpiredPacket() {
    final LinkFilter linkFilter1 = (sourceAccountSettings, sourcePreparePacket, filterChain) -> {
      linkFilter1PreProcessed.set(true);
      InterledgerResponsePacket responsePacket = filterChain.doFilter(sourceAccountSettings, sourcePreparePacket);
      linkFilter1PostProcessed.set(true);
      return responsePacket;
    };
    this.linkFilters.add(linkFilter1);

    final LinkFilter linkFilter2 = (sourceAccountSettings, sourcePreparePacket, filterChain) -> {
      linkFilter2PreProcessed.set(true);
      InterledgerResponsePacket responsePacket = filterChain.doFilter(sourceAccountSettings, sourcePreparePacket);
      linkFilter2PostProcessed.set(true);
      return responsePacket;
    };
    this.linkFilters.add(linkFilter2);

    final LinkFilter linkFilter3 = (sourceAccountSettings, sourcePreparePacket, filterChain) -> {
      linkFilter3PreProcessed.set(true);
      InterledgerResponsePacket responsePacket = filterChain.doFilter(sourceAccountSettings, sourcePreparePacket);
      linkFilter3PostProcessed.set(true);
      return responsePacket;
    };
    this.linkFilters.add(linkFilter3);

    assertThat(this.linkFilters.size()).isEqualTo(3);

    final InterledgerPreparePacket expiredPreparePacket = InterledgerPreparePacket.builder()
      .destination(InterledgerAddress.of("example.foo"))
      .amount(UnsignedLong.ONE)
      .expiresAt(Instant.MIN)
      .executionCondition(InterledgerCondition.of(new byte[32]))
      .build();
    filterChain.doFilter(OUTGOING_ACCOUNT_SETTINGS, expiredPreparePacket).handle(
      fulfillPacket -> fail("Should have rejected but fulfilled!"),
      rejectPacket -> assertThat(rejectPacket.getCode()).isEqualTo(InterledgerErrorCode.R02_INSUFFICIENT_TIMEOUT)
    );

    // All filters should be pre and post processed.
    assertThat(linkFilter1PreProcessed).isTrue();
    assertThat(linkFilter1PostProcessed).isTrue();
    assertThat(linkFilter2PreProcessed).isTrue();
    assertThat(linkFilter2PostProcessed).isTrue();
    assertThat(linkFilter3PreProcessed).isTrue();
    assertThat(linkFilter3PostProcessed).isTrue();
  }

  /**
   * In this test, an exception is thrown in the final portion of the Filter-chain (after all of the filters have been
   * processed) because the CompletableFuture times out. This test is basically verifying the same thing as the above
   * test, but from a slightly different location in the code path. Because the try-catch in the filterChain is very
   * broad (i.e., the entire doFilter is wrapped), we dont need to test _every_ exception source. Instead, two places
   * are chosen to get some coverage, but full coverage is not necessary here.
   *
   * @see "https://github.com/interledger4j/ilpv4-connector/issues/593"
   * @see "https://github.com/interledger4j/ilpv4-connector/issues/588"
   */
  @Test
  public void filterPacketWithExpiredFuture() throws InterruptedException {
    final LinkFilter linkFilter1 = (sourceAccountSettings, sourcePreparePacket, filterChain) -> {
      linkFilter1PreProcessed.set(true);
      InterledgerResponsePacket responsePacket = filterChain.doFilter(sourceAccountSettings, sourcePreparePacket);
      linkFilter1PostProcessed.set(true);
      return responsePacket;
    };
    this.linkFilters.add(linkFilter1);

    final LinkFilter linkFilter2 = (sourceAccountSettings, sourcePreparePacket, filterChain) -> {
      linkFilter2PreProcessed.set(true);
      InterledgerResponsePacket responsePacket = filterChain.doFilter(sourceAccountSettings, sourcePreparePacket);
      linkFilter2PostProcessed.set(true);
      return responsePacket;
    };
    this.linkFilters.add(linkFilter2);

    final LinkFilter linkFilter3 = (sourceAccountSettings, sourcePreparePacket, filterChain) -> {
      linkFilter3PreProcessed.set(true);
      InterledgerResponsePacket responsePacket = filterChain.doFilter(sourceAccountSettings, sourcePreparePacket);
      linkFilter3PostProcessed.set(true);
      return responsePacket;
    };
    this.linkFilters.add(linkFilter3);

    assertThat(this.linkFilters.size()).isEqualTo(3);

    final InterledgerPreparePacket expiredPreparePacket = InterledgerPreparePacket.builder()
      .destination(InterledgerAddress.of("example.foo"))
      .amount(UnsignedLong.ONE)
      .expiresAt(Instant.now().plusMillis(250)) // Enough time to get past the initial expiry check.
      .executionCondition(InterledgerCondition.of(new byte[32]))
      .build();
    filterChain.doFilter(OUTGOING_ACCOUNT_SETTINGS, expiredPreparePacket).handle(
      fulfillPacket -> fail("Should have rejected but fulfilled!"),
      rejectPacket -> assertThat(rejectPacket.getCode()).isEqualTo(InterledgerErrorCode.R00_TRANSFER_TIMED_OUT)
    );

    // All filters should be pre and post processed.
    assertThat(linkFilter1PreProcessed).isTrue();
    assertThat(linkFilter1PostProcessed).isTrue();
    assertThat(linkFilter2PreProcessed).isTrue();
    assertThat(linkFilter2PostProcessed).isTrue();
    assertThat(linkFilter3PreProcessed).isTrue();
    assertThat(linkFilter3PostProcessed).isTrue();
  }

}
