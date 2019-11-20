package org.interledger.connector.packetswitch.filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.interledger.connector.routing.PaymentRouter.PING_ACCOUNT_ID;
import static org.interledger.link.PingLoopbackLink.PING_PROTOCOL_CONDITION;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.caching.AccountSettingsLoadingCache;
import org.interledger.connector.links.LinkManager;
import org.interledger.connector.links.NextHopInfo;
import org.interledger.connector.links.NextHopPacketMapper;
import org.interledger.connector.links.filters.LinkFilter;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.link.Link;
import org.interledger.link.LinkSettings;
import org.interledger.link.LoopbackLink;
import org.interledger.link.PacketRejector;
import org.interledger.link.PingLoopbackLink;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

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

  @Mock
  private List<LinkFilter> linkFiltersMock;
  @Mock
  private LinkManager linkManagerMock;
  @Mock
  private NextHopPacketMapper nextHopPacketMapperMock;
  @Mock
  private AccountSettingsLoadingCache accountSettingsLoadingCacheMock;
  @Mock
  private EventBus eventBus;

  private Link outgoingLink;

  private List<PacketSwitchFilter> packetSwitchFilters;

  private DefaultPacketSwitchFilterChain filterChain;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    this.packetSwitchFilters = Lists.newArrayList();

    this.outgoingLink = new LoopbackLink(
        () -> OPERATOR_ADDRESS,
        OUTGOING_LINK_SETTINGS,
        new PacketRejector(() -> OPERATOR_ADDRESS)
    );

    this.filterChain = new DefaultPacketSwitchFilterChain(
        packetSwitchFilters,
        linkFiltersMock,
        linkManagerMock,
        nextHopPacketMapperMock,
        accountSettingsLoadingCacheMock,
      eventBus);

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

    filterChain.doFilter(INCOMING_ACCOUNT_SETTINGS, PREPARE_PACKET).handle(
        fulfillPacket -> assertThat(fulfillPacket.getFulfillment()).isEqualTo(LoopbackLink.LOOPBACK_FULFILLMENT),
        rejectPacket -> fail("Should have fulfilled but rejected!")
    );

    verify(linkFiltersMock).size();
    verify(linkManagerMock).getOrCreateLink(OUTGOING_ACCOUNT_ID);
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
    when(linkManagerMock.getOrCreateLink(OUTGOING_ACCOUNT_ID)).thenReturn(outgoingLink);
    when(nextHopPacketMapperMock.determineExchangeRate(any(), any(), any())).thenReturn(BigDecimal.ZERO);

    filterChain.doFilter(INCOMING_ACCOUNT_SETTINGS, PREPARE_PACKET).handle(
        fulfillPacket -> assertThat(fulfillPacket.getFulfillment()).isEqualTo(LoopbackLink.LOOPBACK_FULFILLMENT),
        rejectPacket -> fail("Should have fulfilled but rejected!")
    );

    // Each filter should only be called once...
    verify(linkFiltersMock).size();
    verify(linkManagerMock).getOrCreateLink(OUTGOING_ACCOUNT_ID);
    verify(nextHopPacketMapperMock).getNextHopPacket(INCOMING_ACCOUNT_SETTINGS, PREPARE_PACKET);
    verify(nextHopPacketMapperMock).determineExchangeRate(any(), any(), any());

    verifyNoMoreInteractions(nextHopPacketMapperMock);
    verifyNoMoreInteractions(linkFiltersMock);
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
    when(linkManagerMock.getPingLink()).thenReturn(outgoingLink);
    when(nextHopPacketMapperMock.determineExchangeRate(any(), any(), any())).thenReturn(BigDecimal.ZERO);

    filterChain.doFilter(INCOMING_ACCOUNT_SETTINGS, pingPreparePacket).handle(
        fulfillPacket -> assertThat(fulfillPacket.getFulfillment()).isEqualTo(PingLoopbackLink.PING_PROTOCOL_FULFILLMENT),
        rejectPacket -> fail("Should have fulfilled but rejected!")
    );

    verify(linkFiltersMock).size();
    verify(linkManagerMock).getPingLink();
    verify(nextHopPacketMapperMock).getNextHopPacket(INCOMING_ACCOUNT_SETTINGS, pingPreparePacket);
    verify(nextHopPacketMapperMock).determineExchangeRate(any(), any(), any());

    assertThat(this.packetSwitchFilters.size()).isEqualTo(0);
    verifyNoMoreInteractions(nextHopPacketMapperMock);
    verifyNoMoreInteractions(linkFiltersMock);
  }

}
