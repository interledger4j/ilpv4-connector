package com.sappenin.interledger.ilpv4.connector.packetswitch.filters;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.sappenin.interledger.ilpv4.connector.links.LinkManager;
import com.sappenin.interledger.ilpv4.connector.links.NextHopInfo;
import com.sappenin.interledger.ilpv4.connector.links.NextHopPacketMapper;
import com.sappenin.interledger.ilpv4.connector.links.filters.LinkFilter;
import com.sappenin.interledger.ilpv4.connector.links.loopback.LoopbackLink;
import com.sappenin.interledger.ilpv4.connector.links.ping.PingLoopbackLink;
import com.sappenin.interledger.ilpv4.connector.packetswitch.PacketRejector;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.link.AbstractLink;
import org.interledger.connector.link.Link;
import org.interledger.connector.link.LinkSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.core.InterledgerResponsePacketHandler;
import org.interledger.ilpv4.connector.persistence.entities.AccountSettingsEntity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.sappenin.interledger.ilpv4.connector.links.ping.PingLoopbackLink.PING_PROTOCOL_CONDITION;
import static com.sappenin.interledger.ilpv4.connector.routing.PaymentRouter.PING_ACCOUNT_ID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultPacketSwitchFilterChain}.
 */
public class DefaultPacketSwitchFilterChainTest {
  private static final InterledgerAddress OPERATOR_ADDRESS = InterledgerAddress.of("test.operator");

  // The AccountId of the Incoming Link
  private static final AccountId INCOMING_ACCOUNT_ID = AccountId.of("source-account");

  // The AccountId of the Outbound Link
  private static final AccountId OUTGOING_ACCOUNT_ID = AccountId.of("destination-account");

  private static final LinkSettings OUTGOING_LINK_SETTINGS = LinkSettings.builder()
    .linkType(LoopbackLink.LINK_TYPE)
    .putCustomSettings("accountId", OUTGOING_ACCOUNT_ID.value())
    .build();

  private static final InterledgerPreparePacket PREPARE_PACKET = InterledgerPreparePacket.builder()
    .destination(InterledgerAddress.of("test.foo"))
    .amount(BigInteger.ONE)
    .expiresAt(Instant.now().plusSeconds(30))
    .executionCondition(InterledgerCondition.of(new byte[32]))
    .build();

  @Mock
  private List<LinkFilter> linkFiltersMock;
  @Mock
  private LinkManager linkManagerMock;
  @Mock
  private NextHopPacketMapper nextHopPacketMapperMock;

  private Link outgoingLink;

  private List<PacketSwitchFilter> packetSwitchFilters;

  private DefaultPacketSwitchFilterChain filterChain;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    this.packetSwitchFilters = Lists.newArrayList();

    this.outgoingLink = new LoopbackLink(
      () -> Optional.of(OPERATOR_ADDRESS),
      OUTGOING_LINK_SETTINGS,
      new AbstractLink.EventBusEventEmitter(new EventBus()),
      new PacketRejector(() -> Optional.of(OPERATOR_ADDRESS))
    );

    this.filterChain = new DefaultPacketSwitchFilterChain(
      packetSwitchFilters,
      linkFiltersMock,
      linkManagerMock,
      nextHopPacketMapperMock
    );
  }

  @Test
  public void filterPacketWithNoFilters() {
    assertThat(this.packetSwitchFilters.size(), is(0));

    final NextHopInfo nextHopInfo = NextHopInfo.builder()
      .nextHopAccountId(OUTGOING_ACCOUNT_ID)
      .nextHopPacket(PREPARE_PACKET)
      .build();
    when(nextHopPacketMapperMock.getNextHopPacket(eq(INCOMING_ACCOUNT_ID), eq(PREPARE_PACKET))).thenReturn(nextHopInfo);
    when(linkManagerMock.getOrCreateLink(OUTGOING_ACCOUNT_ID)).thenReturn(outgoingLink);

    final AccountSettingsEntity accountSettingsEntityMock = mock(AccountSettingsEntity.class);
    when(accountSettingsEntityMock.getAccountId()).thenReturn(INCOMING_ACCOUNT_ID);

    final InterledgerResponsePacket actual = filterChain.doFilter(accountSettingsEntityMock, PREPARE_PACKET);
    new InterledgerResponsePacketHandler() {

      @Override
      protected void handleFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
        assertThat(interledgerFulfillPacket.getFulfillment(), is(LoopbackLink.LOOPBACK_FULFILLMENT));
      }

      @Override
      protected void handleRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
        fail("Should have fulfilled but rejected!");
      }
    }.handle(actual);

    verify(linkFiltersMock).size();
    verify(linkManagerMock).getOrCreateLink(OUTGOING_ACCOUNT_ID);
    verify(nextHopPacketMapperMock).getNextHopPacket(INCOMING_ACCOUNT_ID, PREPARE_PACKET);

    assertThat(this.packetSwitchFilters.size(), is(0));
    verifyNoMoreInteractions(nextHopPacketMapperMock);
    verifyNoMoreInteractions(linkFiltersMock);
  }

  @Test
  public void filterPacketWithMultipleFilters() {
    final AccountSettingsEntity accountSettingsEntityMock = mock(AccountSettingsEntity.class);
    when(accountSettingsEntityMock.getAccountId()).thenReturn(INCOMING_ACCOUNT_ID);

    final PacketSwitchFilter packetSwitchFilter1 =
      (sourceAccountSettings, sourcePreparePacket, filterChain) -> filterChain
        .doFilter(sourceAccountSettings, sourcePreparePacket);
    this.packetSwitchFilters.add(packetSwitchFilter1);

    final PacketSwitchFilter packetSwitchFilter2 = (sourceAccountSettings, sourcePreparePacket, filterChain) ->
      filterChain.doFilter(sourceAccountSettings, sourcePreparePacket);
    this.packetSwitchFilters.add(packetSwitchFilter2);

    assertThat(this.packetSwitchFilters.size(), is(2));

    final NextHopInfo nextHopInfo = NextHopInfo.builder()
      .nextHopAccountId(OUTGOING_ACCOUNT_ID)
      .nextHopPacket(PREPARE_PACKET)
      .build();
    when(nextHopPacketMapperMock.getNextHopPacket(eq(INCOMING_ACCOUNT_ID), eq(PREPARE_PACKET))).thenReturn(nextHopInfo);
    when(linkManagerMock.getOrCreateLink(OUTGOING_ACCOUNT_ID)).thenReturn(outgoingLink);

    final InterledgerResponsePacket actual = filterChain.doFilter(accountSettingsEntityMock, PREPARE_PACKET);
    new InterledgerResponsePacketHandler() {

      @Override
      protected void handleFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
        assertThat(interledgerFulfillPacket.getFulfillment(), is(LoopbackLink.LOOPBACK_FULFILLMENT));
      }

      @Override
      protected void handleRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
        fail("Should have fulfilled but rejected!");
      }
    }.handle(actual);

    // Each filter should only be called once...
    verify(linkFiltersMock).size();
    verify(linkManagerMock).getOrCreateLink(OUTGOING_ACCOUNT_ID);
    verify(nextHopPacketMapperMock).getNextHopPacket(INCOMING_ACCOUNT_ID, PREPARE_PACKET);

    verifyNoMoreInteractions(nextHopPacketMapperMock);
    verifyNoMoreInteractions(linkFiltersMock);
  }

  /**
   * Validates functionality for the Ping Link.
   */
  @Test
  public void filterPacketForPingLink() {
    assertThat(this.packetSwitchFilters.size(), is(0));

    final InterledgerPreparePacket pingPreparePacket = InterledgerPreparePacket.builder()
      .destination(OPERATOR_ADDRESS)
      .amount(BigInteger.ONE)
      .expiresAt(Instant.now().plusSeconds(30))
      .executionCondition(PING_PROTOCOL_CONDITION)
      .build();

    this.outgoingLink = new PingLoopbackLink(
      () -> Optional.of(OPERATOR_ADDRESS),
      OUTGOING_LINK_SETTINGS,
      new AbstractLink.EventBusEventEmitter(new EventBus())
    );

    final NextHopInfo nextHopInfo = NextHopInfo.builder()
      .nextHopAccountId(PING_ACCOUNT_ID)
      .nextHopPacket(pingPreparePacket)
      .build();
    when(nextHopPacketMapperMock.getNextHopPacket(eq(INCOMING_ACCOUNT_ID), eq(pingPreparePacket)))
      .thenReturn(nextHopInfo);
    when(linkManagerMock.getPingLink()).thenReturn(outgoingLink);

    final AccountSettingsEntity accountSettingsEntityMock = mock(AccountSettingsEntity.class);
    when(accountSettingsEntityMock.getAccountId()).thenReturn(INCOMING_ACCOUNT_ID);

    final InterledgerResponsePacket actual = filterChain.doFilter(accountSettingsEntityMock, pingPreparePacket);
    new InterledgerResponsePacketHandler() {

      @Override
      protected void handleFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
        assertThat(interledgerFulfillPacket.getFulfillment(), is(PingLoopbackLink.PING_PROTOCOL_FULFILLMENT));
      }

      @Override
      protected void handleRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
        fail("Should have fulfilled but rejected!");
      }
    }.handle(actual);


    verify(linkFiltersMock).size();
    verify(linkManagerMock).getPingLink();
    verify(nextHopPacketMapperMock).getNextHopPacket(INCOMING_ACCOUNT_ID, pingPreparePacket);

    assertThat(this.packetSwitchFilters.size(), is(0));
    verifyNoMoreInteractions(nextHopPacketMapperMock);
    verifyNoMoreInteractions(linkFiltersMock);
  }

}
