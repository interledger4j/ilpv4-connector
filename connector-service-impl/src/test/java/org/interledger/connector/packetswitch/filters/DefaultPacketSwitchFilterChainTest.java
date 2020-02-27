package org.interledger.connector.packetswitch.filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.interledger.connector.accounts.sub.LocalDestinationAddressUtils.PING_ACCOUNT_ID;
import static org.interledger.link.PingLoopbackLink.PING_PROTOCOL_CONDITION;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.sub.LocalDestinationAddressUtils;
import org.interledger.connector.caching.AccountSettingsLoadingCache;
import org.interledger.connector.events.PacketEventPublisher;
import org.interledger.connector.links.LinkManager;
import org.interledger.connector.links.NextHopInfo;
import org.interledger.connector.links.NextHopPacketMapper;
import org.interledger.connector.links.filters.LinkFilter;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.link.Link;
import org.interledger.link.LinkSettings;
import org.interledger.link.LoopbackLink;
import org.interledger.link.PacketRejector;
import org.interledger.link.PingLoopbackLink;

import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Unit tests for {@link DefaultPacketSwitchFilterChain}.
 */
public class DefaultPacketSwitchFilterChainTest {

  private static final InterledgerAddress OPERATOR_ADDRESS = InterledgerAddress.of("test.operator");

  // The AccountId of the Incoming Link
  private static final AccountId INCOMING_ACCOUNT_ID = AccountId.of("source-account");
  private static final AccountSettings INCOMING_ACCOUNT_SETTINGS = AccountSettings.builder()
    .accountId(INCOMING_ACCOUNT_ID)
    .accountRelationship(AccountRelationship.PEER)
    .assetCode("USD")
    .assetScale(2)
    .linkType(LoopbackLink.LINK_TYPE)
    .build();

  // The AccountId of the Outbound Link
  private static final AccountId OUTGOING_ACCOUNT_ID = AccountId.of("destination-account");
  private static final AccountSettings OUTGOING_ACCOUNT_SETTINGS = AccountSettings.builder()
    .accountId(OUTGOING_ACCOUNT_ID)
    .accountRelationship(AccountRelationship.PEER)
    .assetCode("USD")
    .assetScale(2)
    .linkType(LoopbackLink.LINK_TYPE)
    .build();

  private static final LinkSettings OUTGOING_LINK_SETTINGS = LinkSettings.builder()
    .linkType(LoopbackLink.LINK_TYPE)
    .putCustomSettings("accountId", OUTGOING_ACCOUNT_ID.value())
    .build();

  private static final AccountSettings PING_ACCOUNT_SETTINGS = AccountSettings.builder()
    .accountId(PING_ACCOUNT_ID)
    .accountRelationship(AccountRelationship.PEER)
    .assetCode("USD")
    .assetScale(2)
    .linkType(PingLoopbackLink.LINK_TYPE)
    .build();

  private static final InterledgerPreparePacket PREPARE_PACKET = InterledgerPreparePacket.builder()
    .destination(InterledgerAddress.of("test.foo"))
    .amount(UnsignedLong.ONE)
    .expiresAt(Instant.now().plusSeconds(30))
    .executionCondition(InterledgerCondition.of(new byte[32]))
    .build();

  private AtomicBoolean packetSwitchFilter1PreProcessed;
  private AtomicBoolean packetSwitchFilter1PostProcessed;
  private AtomicBoolean packetSwitchFilter2PreProcessed;
  private AtomicBoolean packetSwitchFilter2PostProcessed;
  private AtomicBoolean packetSwitchFilter3PreProcessed;
  private AtomicBoolean packetSwitchFilter3PostProcessed;

  @Mock
  private List<LinkFilter> linkFiltersMock;
  @Mock
  private LinkManager linkManagerMock;
  @Mock
  private NextHopPacketMapper nextHopPacketMapperMock;
  @Mock
  private AccountSettingsLoadingCache accountSettingsLoadingCacheMock;
  @Mock
  private LocalDestinationAddressUtils localDestinationAddressUtilsMock;
  @Mock
  private PacketEventPublisher packetEventPublisherMock;

  private Link outgoingLink;
  private List<PacketSwitchFilter> packetSwitchFilters;
  private DefaultPacketSwitchFilterChain filterChain;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    packetSwitchFilter1PreProcessed = new AtomicBoolean(false);
    packetSwitchFilter2PreProcessed = new AtomicBoolean(false);
    packetSwitchFilter3PreProcessed = new AtomicBoolean(false);
    packetSwitchFilter1PostProcessed = new AtomicBoolean(false);
    packetSwitchFilter2PostProcessed = new AtomicBoolean(false);
    packetSwitchFilter3PostProcessed = new AtomicBoolean(false);

    this.packetSwitchFilters = Lists.newArrayList();

    this.outgoingLink = new LoopbackLink(
      () -> OPERATOR_ADDRESS,
      OUTGOING_LINK_SETTINGS,
      new PacketRejector(() -> OPERATOR_ADDRESS)
    );

    this.filterChain = new DefaultPacketSwitchFilterChain(
      new PacketRejector(() -> OPERATOR_ADDRESS),
      packetSwitchFilters,
      linkFiltersMock,
      localDestinationAddressUtilsMock,
      linkManagerMock,
      nextHopPacketMapperMock,
      accountSettingsLoadingCacheMock,
      packetEventPublisherMock
    );

    when(accountSettingsLoadingCacheMock.getAccount(INCOMING_ACCOUNT_ID))
      .thenReturn(Optional.of(INCOMING_ACCOUNT_SETTINGS));
    when(accountSettingsLoadingCacheMock.getAccount(OUTGOING_ACCOUNT_ID))
      .thenReturn(Optional.of(OUTGOING_ACCOUNT_SETTINGS));
    when(accountSettingsLoadingCacheMock.getAccount(PING_ACCOUNT_ID))
      .thenReturn(Optional.of(PING_ACCOUNT_SETTINGS));
  }

  @Test
  public void filterPacketWithNoFilters() {
    assertThat(this.packetSwitchFilters.size()).isEqualTo(0);

    final NextHopInfo nextHopInfo = NextHopInfo.builder()
      .nextHopAccountId(OUTGOING_ACCOUNT_ID)
      .nextHopPacket(PREPARE_PACKET)
      .build();
    when(nextHopPacketMapperMock.getNextHopPacket(eq(INCOMING_ACCOUNT_SETTINGS), eq(PREPARE_PACKET)))
      .thenReturn(nextHopInfo);
    when(linkManagerMock.getOrCreateLink(OUTGOING_ACCOUNT_ID)).thenReturn(outgoingLink);
    when(nextHopPacketMapperMock.determineExchangeRate(any(), any(), any())).thenReturn(BigDecimal.ZERO);

    // Simulate a Ping
    final Link linkMock = mock(Link.class);
    when(linkManagerMock.getOrCreateLink(OUTGOING_ACCOUNT_SETTINGS)).thenReturn(linkMock);
    when(linkMock.sendPacket(any()))
      .thenReturn(InterledgerFulfillPacket.builder().fulfillment(LoopbackLink.LOOPBACK_FULFILLMENT).build());

    filterChain.doFilter(INCOMING_ACCOUNT_SETTINGS, PREPARE_PACKET).handle(
      fulfillPacket -> assertThat(fulfillPacket.getFulfillment()).isEqualTo(LoopbackLink.LOOPBACK_FULFILLMENT),
      rejectPacket -> fail("Should have fulfilled but rejected!")
    );

    verify(linkFiltersMock).size();
    verify(linkManagerMock).getOrCreateLink(OUTGOING_ACCOUNT_SETTINGS);
    verify(nextHopPacketMapperMock).getNextHopPacket(INCOMING_ACCOUNT_SETTINGS, PREPARE_PACKET);
    verify(nextHopPacketMapperMock).determineExchangeRate(any(), any(), any());

    assertThat(this.packetSwitchFilters.size()).isEqualTo(0);
    verifyNoMoreInteractions(nextHopPacketMapperMock);
    verifyNoMoreInteractions(linkFiltersMock);
  }

  @Test
  public void filterPacketWithMultipleFilters() {
    final PacketSwitchFilter packetSwitchFilter1 =
      (sourceAccountSettings, sourcePreparePacket, filterChain) -> filterChain
        .doFilter(sourceAccountSettings, sourcePreparePacket);
    this.packetSwitchFilters.add(packetSwitchFilter1);

    final PacketSwitchFilter packetSwitchFilter2 = (sourceAccountSettings, sourcePreparePacket, filterChain) ->
      filterChain.doFilter(sourceAccountSettings, sourcePreparePacket);
    this.packetSwitchFilters.add(packetSwitchFilter2);

    assertThat(this.packetSwitchFilters.size()).isEqualTo(2);

    final NextHopInfo nextHopInfo = NextHopInfo.builder()
      .nextHopAccountId(OUTGOING_ACCOUNT_ID)
      .nextHopPacket(PREPARE_PACKET)
      .build();
    when(nextHopPacketMapperMock.getNextHopPacket(INCOMING_ACCOUNT_SETTINGS, PREPARE_PACKET)).thenReturn(nextHopInfo);
    when(linkManagerMock.getOrCreateLink(Mockito.<AccountSettings>any())).thenReturn(outgoingLink);
    when(nextHopPacketMapperMock.determineExchangeRate(any(), any(), any())).thenReturn(BigDecimal.ZERO);

    filterChain.doFilter(INCOMING_ACCOUNT_SETTINGS, PREPARE_PACKET).handle(
      fulfillPacket -> assertThat(fulfillPacket.getFulfillment()).isEqualTo(LoopbackLink.LOOPBACK_FULFILLMENT),
      rejectPacket -> fail("Should have fulfilled but rejected!")
    );

    // Each filter should only be called once...
    verify(linkFiltersMock).size();
    verify(linkManagerMock).getOrCreateLink(OUTGOING_ACCOUNT_SETTINGS);
    verify(nextHopPacketMapperMock).getNextHopPacket(INCOMING_ACCOUNT_SETTINGS, PREPARE_PACKET);
    verify(nextHopPacketMapperMock).determineExchangeRate(any(), any(), any());

    verifyNoMoreInteractions(nextHopPacketMapperMock);
    verifyNoMoreInteractions(linkFiltersMock);
  }

  /**
   * In this test, an exception is thrown in the first filter. The test verifies that only filters 1 is processed, but
   * filter 2, 3, and the rest of the filter-chain are un-processed.
   */
  @Test
  public void filterPacketWithExceptionInFirstFilter() {
    final PacketSwitchFilter packetSwitchFilter1 = (sourceAccountSettings, sourcePreparePacket, filterChain) -> {
      packetSwitchFilter1PreProcessed.set(true);
      throw new RuntimeException("Simulated PacketSwitchFilter exception");
    };
    this.packetSwitchFilters.add(packetSwitchFilter1);

    final PacketSwitchFilter packetSwitchFilter2 = (sourceAccountSettings, sourcePreparePacket, filterChain) -> {
      packetSwitchFilter2PreProcessed.set(true);
      InterledgerResponsePacket responsePacket = filterChain.doFilter(sourceAccountSettings, sourcePreparePacket);
      packetSwitchFilter2PostProcessed.set(true);
      return responsePacket;
    };
    this.packetSwitchFilters.add(packetSwitchFilter2);

    final PacketSwitchFilter packetSwitchFilter3 = (sourceAccountSettings, sourcePreparePacket, filterChain) -> {
      packetSwitchFilter3PreProcessed.set(true);
      InterledgerResponsePacket responsePacket = filterChain.doFilter(sourceAccountSettings, sourcePreparePacket);
      packetSwitchFilter3PostProcessed.set(true);
      return responsePacket;
    };
    this.packetSwitchFilters.add(packetSwitchFilter3);

    assertThat(this.packetSwitchFilters.size()).isEqualTo(3);

    final NextHopInfo nextHopInfo = NextHopInfo.builder()
      .nextHopAccountId(OUTGOING_ACCOUNT_ID)
      .nextHopPacket(PREPARE_PACKET)
      .build();
    when(nextHopPacketMapperMock.getNextHopPacket(INCOMING_ACCOUNT_SETTINGS, PREPARE_PACKET)).thenReturn(nextHopInfo);
    when(linkManagerMock.getOrCreateLink(Mockito.<AccountSettings>any())).thenReturn(outgoingLink);
    when(nextHopPacketMapperMock.determineExchangeRate(any(), any(), any())).thenReturn(BigDecimal.ZERO);

    filterChain.doFilter(INCOMING_ACCOUNT_SETTINGS, PREPARE_PACKET).handle(
      fulfillPacket -> fail("Should have rejected but fulfilled!"),
      rejectPacket -> assertThat(rejectPacket.getCode()).isEqualTo(InterledgerErrorCode.T00_INTERNAL_ERROR)
    );

    // Exception in first filter means 2 and 3 don't get called.
    assertThat(packetSwitchFilter1PreProcessed).isTrue();
    assertThat(packetSwitchFilter1PostProcessed).isFalse();
    assertThat(packetSwitchFilter2PreProcessed).isFalse();
    assertThat(packetSwitchFilter2PostProcessed).isFalse();
    assertThat(packetSwitchFilter3PreProcessed).isFalse();
    assertThat(packetSwitchFilter3PostProcessed).isFalse();

    verifyNoInteractions(nextHopPacketMapperMock);
    verifyNoInteractions(linkFiltersMock);
    verifyNoInteractions(accountSettingsLoadingCacheMock);
    verifyNoInteractions(packetEventPublisherMock);
  }

  /**
   * In this test, an exception is thrown in the second filter. The test verifies that only filter 1 and 2 are
   * processed, but filter 3 and the rest of the filter-chain are un-processed.
   */
  @Test
  public void filterPacketWithExceptionInMiddleFilter() {
    final PacketSwitchFilter packetSwitchFilter1 = (sourceAccountSettings, sourcePreparePacket, filterChain) -> {
      packetSwitchFilter1PreProcessed.set(true);
      InterledgerResponsePacket responsePacket = filterChain.doFilter(sourceAccountSettings, sourcePreparePacket);
      packetSwitchFilter1PostProcessed.set(true);
      return responsePacket;
    };
    this.packetSwitchFilters.add(packetSwitchFilter1);

    final PacketSwitchFilter packetSwitchFilter2 = (sourceAccountSettings, sourcePreparePacket, filterChain) -> {
      packetSwitchFilter2PreProcessed.set(true);
      throw new RuntimeException("Simulated PacketSwitchFilter exception");
    };
    this.packetSwitchFilters.add(packetSwitchFilter2);

    final PacketSwitchFilter packetSwitchFilter3 = (sourceAccountSettings, sourcePreparePacket, filterChain) -> {
      packetSwitchFilter3PreProcessed.set(true);
      InterledgerResponsePacket responsePacket = filterChain.doFilter(sourceAccountSettings, sourcePreparePacket);
      packetSwitchFilter3PostProcessed.set(true);
      return responsePacket;
    };
    this.packetSwitchFilters.add(packetSwitchFilter3);

    assertThat(this.packetSwitchFilters.size()).isEqualTo(3);

    final NextHopInfo nextHopInfo = NextHopInfo.builder()
      .nextHopAccountId(OUTGOING_ACCOUNT_ID)
      .nextHopPacket(PREPARE_PACKET)
      .build();
    when(nextHopPacketMapperMock.getNextHopPacket(INCOMING_ACCOUNT_SETTINGS, PREPARE_PACKET)).thenReturn(nextHopInfo);
    when(linkManagerMock.getOrCreateLink(Mockito.<AccountSettings>any())).thenReturn(outgoingLink);
    when(nextHopPacketMapperMock.determineExchangeRate(any(), any(), any())).thenReturn(BigDecimal.ZERO);

    filterChain.doFilter(INCOMING_ACCOUNT_SETTINGS, PREPARE_PACKET).handle(
      fulfillPacket -> fail("Should have rejected but fulfilled!"),
      rejectPacket -> assertThat(rejectPacket.getCode()).isEqualTo(InterledgerErrorCode.T00_INTERNAL_ERROR)
    );

    // Exception in second filter means 1 and 2 get called, but not 3.
    assertThat(packetSwitchFilter1PreProcessed).isTrue();
    assertThat(packetSwitchFilter1PostProcessed).isTrue();
    assertThat(packetSwitchFilter2PreProcessed).isTrue();
    assertThat(packetSwitchFilter2PostProcessed).isFalse();
    assertThat(packetSwitchFilter3PreProcessed).isFalse();
    assertThat(packetSwitchFilter3PostProcessed).isFalse();

    verifyNoInteractions(nextHopPacketMapperMock);
    verifyNoInteractions(linkFiltersMock);
    verifyNoInteractions(accountSettingsLoadingCacheMock);
    verifyNoInteractions(packetEventPublisherMock);
  }

  /**
   * In this test, an exception is thrown in the last filter. The test verifies that filters 1 and 2 are still
   * processed.
   */
  @Test
  public void filterPacketWithExceptionInLastFilter() {
    final PacketSwitchFilter packetSwitchFilter1 = (sourceAccountSettings, sourcePreparePacket, filterChain) -> {
      packetSwitchFilter1PreProcessed.set(true);
      InterledgerResponsePacket responsePacket = filterChain.doFilter(sourceAccountSettings, sourcePreparePacket);
      packetSwitchFilter1PostProcessed.set(true);
      return responsePacket;
    };
    this.packetSwitchFilters.add(packetSwitchFilter1);

    final PacketSwitchFilter packetSwitchFilter2 = (sourceAccountSettings, sourcePreparePacket, filterChain) -> {
      packetSwitchFilter2PreProcessed.set(true);
      InterledgerResponsePacket responsePacket = filterChain.doFilter(sourceAccountSettings, sourcePreparePacket);
      packetSwitchFilter2PostProcessed.set(true);
      return responsePacket;
    };
    this.packetSwitchFilters.add(packetSwitchFilter2);

    final PacketSwitchFilter packetSwitchFilter3 = (sourceAccountSettings, sourcePreparePacket, filterChain) -> {
      packetSwitchFilter3PreProcessed.set(true);
      throw new RuntimeException("Simulated PacketSwitchFilter exception");
    };
    this.packetSwitchFilters.add(packetSwitchFilter3);

    assertThat(this.packetSwitchFilters.size()).isEqualTo(3);

    final NextHopInfo nextHopInfo = NextHopInfo.builder()
      .nextHopAccountId(OUTGOING_ACCOUNT_ID)
      .nextHopPacket(PREPARE_PACKET)
      .build();
    when(nextHopPacketMapperMock.getNextHopPacket(INCOMING_ACCOUNT_SETTINGS, PREPARE_PACKET)).thenReturn(nextHopInfo);
    when(linkManagerMock.getOrCreateLink(Mockito.<AccountSettings>any())).thenReturn(outgoingLink);
    when(nextHopPacketMapperMock.determineExchangeRate(any(), any(), any())).thenReturn(BigDecimal.ZERO);

    filterChain.doFilter(INCOMING_ACCOUNT_SETTINGS, PREPARE_PACKET).handle(
      fulfillPacket -> fail("Should have rejected but fulfilled!"),
      rejectPacket -> assertThat(rejectPacket.getCode()).isEqualTo(InterledgerErrorCode.T00_INTERNAL_ERROR)
    );

    // Exception in last filter means 1 and 2 get called, but only half of 3.
    assertThat(packetSwitchFilter1PreProcessed).isTrue();
    assertThat(packetSwitchFilter1PostProcessed).isTrue();
    assertThat(packetSwitchFilter2PreProcessed).isTrue();
    assertThat(packetSwitchFilter2PostProcessed).isTrue();
    assertThat(packetSwitchFilter3PreProcessed).isTrue();
    assertThat(packetSwitchFilter3PostProcessed).isFalse();

    verifyNoInteractions(nextHopPacketMapperMock);
    verifyNoInteractions(linkFiltersMock);
    verifyNoInteractions(accountSettingsLoadingCacheMock);
    verifyNoInteractions(packetEventPublisherMock);
  }

  /**
   * In this test, an exception is thrown from the NextHopPacketMapper, in the final portion of the Filter-chain (after
   * all of the filters have been processed). This test verifies that all filters are processed on the return-path. For
   * example, the Balance Tracking filter always needs to process both sides of a call. If the FilterChain aborts this
   * processing, then it's possible that balance tracking logic will be incorrect.
   *
   * @see "https://github.com/interledger4j/ilpv4-connector/issues/593"
   * @see "https://github.com/interledger4j/ilpv4-connector/issues/588"
   */
  @Test
  public void filterPacketWithExpiredPacket() {
    final PacketSwitchFilter packetSwitchFilter1 = (sourceAccountSettings, sourcePreparePacket, filterChain) -> {
      packetSwitchFilter1PreProcessed.set(true);
      InterledgerResponsePacket responsePacket = filterChain.doFilter(sourceAccountSettings, sourcePreparePacket);
      packetSwitchFilter1PostProcessed.set(true);
      return responsePacket;
    };
    this.packetSwitchFilters.add(packetSwitchFilter1);

    final PacketSwitchFilter packetSwitchFilter2 = (sourceAccountSettings, sourcePreparePacket, filterChain) -> {
      packetSwitchFilter2PreProcessed.set(true);
      InterledgerResponsePacket responsePacket = filterChain.doFilter(sourceAccountSettings, sourcePreparePacket);
      packetSwitchFilter2PostProcessed.set(true);
      return responsePacket;
    };
    this.packetSwitchFilters.add(packetSwitchFilter2);

    final PacketSwitchFilter packetSwitchFilter3 = (sourceAccountSettings, sourcePreparePacket, filterChain) -> {
      packetSwitchFilter3PreProcessed.set(true);
      InterledgerResponsePacket responsePacket = filterChain.doFilter(sourceAccountSettings, sourcePreparePacket);
      packetSwitchFilter3PostProcessed.set(true);
      return responsePacket;
    };
    this.packetSwitchFilters.add(packetSwitchFilter3);

    assertThat(this.packetSwitchFilters.size()).isEqualTo(3);

    // Simulate an exception in NextHopPacketMapper (e.g., an expired packet).
    when(nextHopPacketMapperMock.getNextHopPacket(INCOMING_ACCOUNT_SETTINGS, PREPARE_PACKET))
      .thenThrow(new RuntimeException("Simulated Exception"));
    when(linkManagerMock.getOrCreateLink(Mockito.<AccountSettings>any())).thenReturn(outgoingLink);
    when(nextHopPacketMapperMock.determineExchangeRate(any(), any(), any())).thenReturn(BigDecimal.ZERO);

    filterChain.doFilter(INCOMING_ACCOUNT_SETTINGS, PREPARE_PACKET).handle(
      fulfillPacket -> fail("Should have rejected but fulfilled!"),
      rejectPacket -> assertThat(rejectPacket.getCode()).isEqualTo(InterledgerErrorCode.T00_INTERNAL_ERROR)
    );

    // All filters should be pre and post processed.
    assertThat(packetSwitchFilter1PreProcessed).isTrue();
    assertThat(packetSwitchFilter1PostProcessed).isTrue();
    assertThat(packetSwitchFilter2PreProcessed).isTrue();
    assertThat(packetSwitchFilter2PostProcessed).isTrue();
    assertThat(packetSwitchFilter3PreProcessed).isTrue();
    assertThat(packetSwitchFilter3PostProcessed).isTrue();

    verify(nextHopPacketMapperMock).getNextHopPacket(INCOMING_ACCOUNT_SETTINGS, PREPARE_PACKET);
    verifyNoMoreInteractions(nextHopPacketMapperMock);
    verifyNoInteractions(linkFiltersMock);
    verifyNoInteractions(accountSettingsLoadingCacheMock);
    verifyNoInteractions(packetEventPublisherMock);
  }

  /**
   * In this test, an exception is thrown in the final portion of the Filter-chain (after all of the filters have been
   * processed) but from a source that is slightly different from {@link #filterPacketWithExpiredPacket}. This test is
   * basically verifying the same thing as that test, but from a slightly different location in the code path. Because
   * the try-catch in the filterChain is very broad (i.e., the entire doFilter is wrapped), we dont need to test _every_
   * exception source. Instead, two places are chosen to get some coverage, but full coverage is not necessary here.
   *
   * @see "https://github.com/interledger4j/ilpv4-connector/issues/593"
   * @see "https://github.com/interledger4j/ilpv4-connector/issues/588"
   */
  @Test
  public void filterPacketWithInvalidFx() {
    final PacketSwitchFilter packetSwitchFilter1 = (sourceAccountSettings, sourcePreparePacket, filterChain) -> {
      packetSwitchFilter1PreProcessed.set(true);
      InterledgerResponsePacket responsePacket = filterChain.doFilter(sourceAccountSettings, sourcePreparePacket);
      packetSwitchFilter1PostProcessed.set(true);
      return responsePacket;
    };
    this.packetSwitchFilters.add(packetSwitchFilter1);

    final PacketSwitchFilter packetSwitchFilter2 = (sourceAccountSettings, sourcePreparePacket, filterChain) -> {
      packetSwitchFilter2PreProcessed.set(true);
      InterledgerResponsePacket responsePacket = filterChain.doFilter(sourceAccountSettings, sourcePreparePacket);
      packetSwitchFilter2PostProcessed.set(true);
      return responsePacket;
    };
    this.packetSwitchFilters.add(packetSwitchFilter2);

    final PacketSwitchFilter packetSwitchFilter3 = (sourceAccountSettings, sourcePreparePacket, filterChain) -> {
      packetSwitchFilter3PreProcessed.set(true);
      InterledgerResponsePacket responsePacket = filterChain.doFilter(sourceAccountSettings, sourcePreparePacket);
      packetSwitchFilter3PostProcessed.set(true);
      return responsePacket;
    };
    this.packetSwitchFilters.add(packetSwitchFilter3);

    assertThat(this.packetSwitchFilters.size()).isEqualTo(3);

    final NextHopInfo nextHopInfo = NextHopInfo.builder()
      .nextHopAccountId(PING_ACCOUNT_ID)
      .nextHopPacket(PREPARE_PACKET)
      .build();
    when(nextHopPacketMapperMock.getNextHopPacket(eq(INCOMING_ACCOUNT_SETTINGS), eq(PREPARE_PACKET)))
      .thenReturn(nextHopInfo);
    // Simulate an exception in the Link manager.
    when(linkManagerMock.getOrCreateLink(Mockito.<AccountSettings>any()))
      .thenThrow(new RuntimeException("Simulated Link Exception"));
    when(nextHopPacketMapperMock.determineExchangeRate(any(), any(), any())).thenReturn(BigDecimal.ZERO);

    filterChain.doFilter(INCOMING_ACCOUNT_SETTINGS, PREPARE_PACKET).handle(
      fulfillPacket -> fail("Should have rejected but fulfilled!"),
      rejectPacket -> assertThat(rejectPacket.getCode()).isEqualTo(InterledgerErrorCode.T00_INTERNAL_ERROR)
    );

    // All filters should be pre and post processed.
    assertThat(packetSwitchFilter1PreProcessed).isTrue();
    assertThat(packetSwitchFilter1PostProcessed).isTrue();
    assertThat(packetSwitchFilter2PreProcessed).isTrue();
    assertThat(packetSwitchFilter2PostProcessed).isTrue();
    assertThat(packetSwitchFilter3PreProcessed).isTrue();
    assertThat(packetSwitchFilter3PostProcessed).isTrue();

    verify(nextHopPacketMapperMock).getNextHopPacket(INCOMING_ACCOUNT_SETTINGS, PREPARE_PACKET);
    verify(accountSettingsLoadingCacheMock).getAccount(any());
    verifyNoMoreInteractions(nextHopPacketMapperMock);
    verifyNoMoreInteractions(accountSettingsLoadingCacheMock);
    verifyNoInteractions(linkFiltersMock);
    verifyNoInteractions(packetEventPublisherMock);
  }

  /**
   * Validates functionality for the Ping Link.
   */
  @Test
  public void filterPacketForPingLink() {
    assertThat(this.packetSwitchFilters.size()).isEqualTo(0);

    final InterledgerPreparePacket pingPreparePacket = InterledgerPreparePacket.builder()
      .destination(OPERATOR_ADDRESS)
      .amount(UnsignedLong.ONE)
      .expiresAt(Instant.now().plusSeconds(30))
      .executionCondition(PING_PROTOCOL_CONDITION)
      .build();

    this.outgoingLink = new PingLoopbackLink(
      () -> OPERATOR_ADDRESS, OUTGOING_LINK_SETTINGS
    );

    final NextHopInfo nextHopInfo = NextHopInfo.builder()
      .nextHopAccountId(PING_ACCOUNT_ID)
      .nextHopPacket(pingPreparePacket)
      .build();
    when(nextHopPacketMapperMock.getNextHopPacket(eq(INCOMING_ACCOUNT_SETTINGS), eq(pingPreparePacket)))
      .thenReturn(nextHopInfo);
    when(nextHopPacketMapperMock.determineExchangeRate(any(), any(), any())).thenReturn(BigDecimal.ZERO);

    // Simulate a Ping
    when(localDestinationAddressUtilsMock.isLocalDestinationAddress(OPERATOR_ADDRESS)).thenReturn(true);
    final Link pingLink = mock(Link.class);
    when(linkManagerMock.getOrCreateLink(Mockito.<AccountSettings>any())).thenReturn(pingLink);
    when(pingLink.sendPacket(pingPreparePacket))
      .thenReturn(InterledgerFulfillPacket.builder().fulfillment(PingLoopbackLink.PING_PROTOCOL_FULFILLMENT).build());

    filterChain.doFilter(INCOMING_ACCOUNT_SETTINGS, pingPreparePacket).handle(
      fulfillPacket -> assertThat(fulfillPacket.getFulfillment()).isEqualTo(PingLoopbackLink.PING_PROTOCOL_FULFILLMENT),
      rejectPacket -> fail("Should have fulfilled but rejected!")
    );

    verify(linkFiltersMock).size();
    verify(nextHopPacketMapperMock).getNextHopPacket(INCOMING_ACCOUNT_SETTINGS, pingPreparePacket);
    verify(nextHopPacketMapperMock).determineExchangeRate(any(), any(), any());

    assertThat(this.packetSwitchFilters.size()).isEqualTo(0);
    verifyNoMoreInteractions(nextHopPacketMapperMock);
    verifyNoMoreInteractions(linkFiltersMock);
  }

  /**
   * Tests only the local vs forwarding functionality of the filter chain when an account IS a locally fulfilled SPSP
   * account.
   */
  @Test
  public void testDoFilterWithLocalSpspAddress() {
    final InterledgerPreparePacket pingPreparePacket = InterledgerPreparePacket.builder()
      .destination(OPERATOR_ADDRESS)
      .amount(UnsignedLong.ONE)
      .expiresAt(Instant.now().plusSeconds(30))
      .executionCondition(PING_PROTOCOL_CONDITION)
      .build();

    this.outgoingLink = new PingLoopbackLink(
      () -> OPERATOR_ADDRESS, OUTGOING_LINK_SETTINGS
    );

    when(nextHopPacketMapperMock.getNextHopPacket(eq(INCOMING_ACCOUNT_SETTINGS), eq(pingPreparePacket)))
      .thenReturn(
        NextHopInfo.builder()
          .nextHopAccountId(OUTGOING_ACCOUNT_ID)
          .nextHopPacket(pingPreparePacket)
          .build()
      );
    when(nextHopPacketMapperMock.determineExchangeRate(any(), any(), any())).thenReturn(BigDecimal.ZERO);

    when(localDestinationAddressUtilsMock.isLocalSpspDestinationAddress(any())).thenReturn(true);
    when(linkManagerMock.getOrCreateSpspReceiverLink(any())).thenReturn(outgoingLink);

    filterChain.doFilter(INCOMING_ACCOUNT_SETTINGS, pingPreparePacket).handle(
      fulfillPacket -> assertThat(fulfillPacket.getFulfillment()).isEqualTo(PingLoopbackLink.PING_PROTOCOL_FULFILLMENT),
      rejectPacket -> fail("Should have fulfilled but rejected!")
    );

    verify(localDestinationAddressUtilsMock).isLocalSpspDestinationAddress(OPERATOR_ADDRESS);
    verifyNoMoreInteractions(localDestinationAddressUtilsMock);
    verify(linkManagerMock).getOrCreateSpspReceiverLink(OUTGOING_ACCOUNT_SETTINGS);
    verifyNoMoreInteractions(linkManagerMock);
  }

  /**
   * Tests only the local vs forwarding functionality of the filter chain when an account IS NOT a locally fulfilled
   * SPSP account.
   */
  @Test
  public void testDoFilterWithNonLocalSpspAddress() {
    final InterledgerAddress destinationAddres = InterledgerAddress.of("example.foo.bar.baz");
    final InterledgerPreparePacket pingPreparePacket = InterledgerPreparePacket.builder()
      .destination(destinationAddres)
      .amount(UnsignedLong.ONE)
      .expiresAt(Instant.now().plusSeconds(30))
      .executionCondition(PING_PROTOCOL_CONDITION)
      .build();

    this.outgoingLink = new PingLoopbackLink(
      () -> OPERATOR_ADDRESS, OUTGOING_LINK_SETTINGS
    );

    when(nextHopPacketMapperMock.getNextHopPacket(eq(INCOMING_ACCOUNT_SETTINGS), eq(pingPreparePacket)))
      .thenReturn(
        NextHopInfo.builder()
          .nextHopAccountId(OUTGOING_ACCOUNT_ID)
          .nextHopPacket(pingPreparePacket)
          .build()
      );
    when(nextHopPacketMapperMock.determineExchangeRate(any(), any(), any())).thenReturn(BigDecimal.ZERO);

    when(localDestinationAddressUtilsMock.isLocalSpspDestinationAddress(any())).thenReturn(false);
    when(linkManagerMock.getOrCreateLink(Mockito.<AccountSettings>any())).thenReturn(outgoingLink);

    filterChain.doFilter(INCOMING_ACCOUNT_SETTINGS, pingPreparePacket).handle(
      fulfillPacket -> assertThat(fulfillPacket.getFulfillment()).isEqualTo(PingLoopbackLink.PING_PROTOCOL_FULFILLMENT),
      rejectPacket -> fail("Should have fulfilled but rejected!")
    );

    verify(localDestinationAddressUtilsMock).isLocalSpspDestinationAddress(destinationAddres);
    verifyNoMoreInteractions(localDestinationAddressUtilsMock);
    verify(linkManagerMock).getOrCreateLink(OUTGOING_ACCOUNT_SETTINGS);
    verifyNoMoreInteractions(linkManagerMock);
  }

  @Test
  public void computeLinkForLocalSpsp() {
    when(localDestinationAddressUtilsMock.isLocalSpspDestinationAddress(any())).thenReturn(true);
    filterChain.computeLink(INCOMING_ACCOUNT_SETTINGS, InterledgerAddress.of("example.foo"));
    verify(linkManagerMock).getOrCreateSpspReceiverLink(INCOMING_ACCOUNT_SETTINGS);
    verifyNoMoreInteractions(linkManagerMock);
  }

  @Test
  public void computeLinkForForwardingAccount() {
    when(localDestinationAddressUtilsMock.isLocalSpspDestinationAddress(any())).thenReturn(false);
    filterChain.computeLink(INCOMING_ACCOUNT_SETTINGS, InterledgerAddress.of("example.foo"));
    verify(linkManagerMock).getOrCreateLink(INCOMING_ACCOUNT_SETTINGS);
    verifyNoMoreInteractions(linkManagerMock);
  }
}
