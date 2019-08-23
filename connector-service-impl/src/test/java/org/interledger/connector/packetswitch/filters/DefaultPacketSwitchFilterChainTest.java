package org.interledger.connector.packetswitch.filters;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.link.AbstractLink;
import org.interledger.connector.link.Link;
import org.interledger.connector.link.LinkSettings;
import org.interledger.connector.links.LinkManager;
import org.interledger.connector.links.NextHopInfo;
import org.interledger.connector.links.NextHopPacketMapper;
import org.interledger.connector.links.filters.LinkFilter;
import org.interledger.connector.links.loopback.LoopbackLink;
import org.interledger.connector.links.ping.PingLoopbackLink;
import org.interledger.connector.packetswitch.PacketRejector;
import org.interledger.connector.persistence.entities.AccountSettingsEntity;
import org.interledger.connector.persistence.repositories.AccountSettingsRepository;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerPreparePacket;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.interledger.connector.links.ping.PingLoopbackLink.PING_PROTOCOL_CONDITION;
import static org.interledger.connector.routing.PaymentRouter.PING_ACCOUNT_ID;
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
  @Mock
  private AccountSettingsRepository accountSettingsRepositoryMock;

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
      nextHopPacketMapperMock,
      accountSettingsRepositoryMock
    );

    when(accountSettingsRepositoryMock.findByAccountId(INCOMING_ACCOUNT_ID))
      .thenReturn(Optional.of(new AccountSettingsEntity(INCOMING_ACCOUNT_SETTINGS)));
    when(accountSettingsRepositoryMock.findByAccountId(OUTGOING_ACCOUNT_ID))
      .thenReturn(Optional.of(new AccountSettingsEntity(OUTGOING_ACCOUNT_SETTINGS)));
    when(accountSettingsRepositoryMock.findByAccountId(PING_ACCOUNT_ID))
      .thenReturn(Optional.of(new AccountSettingsEntity(PING_ACCOUNT_SETTINGS)));
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

    filterChain.doFilter(accountSettingsEntityMock, PREPARE_PACKET).handle(
      fulfillPacket -> assertThat(fulfillPacket.getFulfillment(), is(LoopbackLink.LOOPBACK_FULFILLMENT)),
      rejectPacket -> fail("Should have fulfilled but rejected!")
    );

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

    filterChain.doFilter(accountSettingsEntityMock, PREPARE_PACKET).handle(
      fulfillPacket -> assertThat(fulfillPacket.getFulfillment(), is(LoopbackLink.LOOPBACK_FULFILLMENT)),
      rejectPacket -> fail("Should have fulfilled but rejected!")
    );

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

    filterChain.doFilter(INCOMING_ACCOUNT_SETTINGS, pingPreparePacket).handle(
      fulfillPacket -> assertThat(fulfillPacket.getFulfillment(), is(PingLoopbackLink.PING_PROTOCOL_FULFILLMENT)),
      rejectPacket -> fail("Should have fulfilled but rejected!")
    );

    verify(linkFiltersMock).size();
    verify(linkManagerMock).getPingLink();
    verify(nextHopPacketMapperMock).getNextHopPacket(INCOMING_ACCOUNT_ID, pingPreparePacket);

    assertThat(this.packetSwitchFilters.size(), is(0));
    verifyNoMoreInteractions(nextHopPacketMapperMock);
    verifyNoMoreInteractions(linkFiltersMock);
  }

}
