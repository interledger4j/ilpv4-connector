package com.sappenin.interledger.ilpv4.connector.packetswitch;

import com.google.common.cache.LoadingCache;
import com.google.common.eventbus.EventBus;
import com.sappenin.interledger.ilpv4.connector.ConnectorExceptionHandler;
import com.sappenin.interledger.ilpv4.connector.links.LinkManager;
import com.sappenin.interledger.ilpv4.connector.links.NextHopInfo;
import com.sappenin.interledger.ilpv4.connector.links.NextHopPacketMapper;
import com.sappenin.interledger.ilpv4.connector.links.filters.LinkFilter;
import com.sappenin.interledger.ilpv4.connector.links.loopback.LoopbackLink;
import com.sappenin.interledger.ilpv4.connector.packetswitch.filters.PacketSwitchFilter;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.link.AbstractLink;
import org.interledger.connector.link.Link;
import org.interledger.connector.link.LinkSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.core.InterledgerResponsePacketHandler;
import org.interledger.ilpv4.connector.persistence.entities.AccountSettingsEntity;
import org.interledger.ilpv4.connector.persistence.repositories.AccountSettingsRepository;
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
import static org.interledger.core.InterledgerErrorCode.T00_INTERNAL_ERROR;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultILPv4PacketSwitch}.
 */
public class DefaultILPv4PacketSwitchTest {

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
  private List<PacketSwitchFilter> packetSwitchFiltersMock;
  @Mock
  private List<LinkFilter> linkFiltersMock;
  @Mock
  private LinkManager linkManagerMock;
  @Mock
  private NextHopPacketMapper nextHopPacketMapperMock;
  @Mock
  private ConnectorExceptionHandler connectorExceptionHandlerMock;
  @Mock
  private AccountSettingsRepository accountSettingsRepositoryMock;
  @Mock
  private PacketRejector packetRejectorMock;

  private Link outgoingLink;

  private DefaultILPv4PacketSwitch packetSwitch;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    this.outgoingLink = new LoopbackLink(
      () -> Optional.of(OPERATOR_ADDRESS),
      OUTGOING_LINK_SETTINGS,
      new AbstractLink.EventBusEventEmitter(new EventBus()),
      new PacketRejector(() -> Optional.of(OPERATOR_ADDRESS))
    );

    this.packetSwitch = new DefaultILPv4PacketSwitch(
      packetSwitchFiltersMock,
      linkFiltersMock,
      linkManagerMock,
      nextHopPacketMapperMock,
      connectorExceptionHandlerMock,
      accountSettingsRepositoryMock,
      packetRejectorMock
    );
  }

  /**
   * Validate the PacketSwitch when the supplied account does not exist.
   */
  @Test(expected = InterledgerProtocolException.class)
  public void switchPacketWithNoAccount() {
    final AccountId NON_EXISTENT_ACCOUNT_ID = AccountId.of("123");

    final AccountSettingsEntity accountSettingsEntityMock = mock(AccountSettingsEntity.class);
    when(accountSettingsEntityMock.getAccountId()).thenReturn(NON_EXISTENT_ACCOUNT_ID);

    when(accountSettingsRepositoryMock.findByAccountId(any())).thenReturn(Optional.empty());

    final InterledgerRejectPacket rejectPacket = InterledgerRejectPacket.builder()
      .code(T00_INTERNAL_ERROR)
      .message("")
      .build();
    when(packetRejectorMock.reject(any(), any(), any(), anyString())).thenReturn(rejectPacket);

    try {
      packetSwitch.switchPacket(NON_EXISTENT_ACCOUNT_ID, PREPARE_PACKET);
      fail("Should have thrown an InterledgerProtocolException!");
    } catch (InterledgerProtocolException e) {
      verify(accountSettingsRepositoryMock).findByAccountId(NON_EXISTENT_ACCOUNT_ID);
      verify(packetRejectorMock).reject(
        eq(NON_EXISTENT_ACCOUNT_ID),
        eq(PREPARE_PACKET),
        eq(T00_INTERNAL_ERROR),
        eq("No Account found: `123`")
      );

      assertThat(e.getInterledgerRejectPacket(), is(rejectPacket));

      verifyZeroInteractions(connectorExceptionHandlerMock);
      verifyZeroInteractions(nextHopPacketMapperMock);
      verifyZeroInteractions(linkFiltersMock);
      verifyZeroInteractions(linkManagerMock);
      verifyZeroInteractions(packetSwitchFiltersMock);
      verifyZeroInteractions(packetRejectorMock);
      verifyNoMoreInteractions(accountSettingsRepositoryMock);
      throw e;
    }
  }

  /**
   * Validate the PacketSwitch with the same account multiple times, and assert that the Cache is engaged (i.e.,
   * AccountSettingsRepository is engaged only once despite five packets).
   */
  @Test
  public void switchPacketMultipleTimeWithSameAccount() {
    final AccountSettingsEntity accountSettingsEntityMock = mock(AccountSettingsEntity.class);
    when(accountSettingsEntityMock.getAccountId()).thenReturn(INCOMING_ACCOUNT_ID);

    when(accountSettingsRepositoryMock.findByAccountId(any())).thenReturn(Optional.of(accountSettingsEntityMock));

    // No FX/expiry changes, to keep things simple.
    final NextHopInfo nextHopInfo = NextHopInfo.builder()
      .nextHopAccountId(OUTGOING_ACCOUNT_ID)
      .nextHopPacket(PREPARE_PACKET)
      .build();
    when(nextHopPacketMapperMock.getNextHopPacket(eq(INCOMING_ACCOUNT_ID), eq(PREPARE_PACKET))).thenReturn(nextHopInfo);
    when(linkManagerMock.getOrCreateLink(OUTGOING_ACCOUNT_ID)).thenReturn(outgoingLink);

    // Do the send numReps times to prove that the cache is working...
    final int numReps = 5;
    for (int i = 0; i < numReps; i++) {
      final InterledgerResponsePacket actual = packetSwitch.switchPacket(INCOMING_ACCOUNT_ID, PREPARE_PACKET);
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
    }

    verify(packetSwitchFiltersMock, times(numReps)).size();
    verify(linkFiltersMock, times(numReps)).size();
    verify(linkManagerMock, times(numReps)).getOrCreateLink(OUTGOING_ACCOUNT_ID);
    verify(nextHopPacketMapperMock, times(numReps)).getNextHopPacket(INCOMING_ACCOUNT_ID, PREPARE_PACKET);
    verify(accountSettingsRepositoryMock, times(2)).findByAccountId(any());

    verifyZeroInteractions(connectorExceptionHandlerMock);
    verifyZeroInteractions(packetRejectorMock);
    verifyNoMoreInteractions(packetSwitchFiltersMock);
    verifyNoMoreInteractions(nextHopPacketMapperMock);
    verifyNoMoreInteractions(linkFiltersMock);
    verifyNoMoreInteractions(accountSettingsRepositoryMock);
  }

  /**
   * Validate the PacketSwitch with the five different accounts and assert that the Cache is not engaged (i.e.,
   * AccountSettingsRepository is engaged five times).
   */
  @Test
  public void switchPacketMultipleTimeWithDifferentAccounts() {
    final AccountSettingsEntity accountSettingsEntityMock = mock(AccountSettingsEntity.class);

    final int numReps = 5;
    for (int i = 0; i < numReps; i++) {
      final AccountId incomingAccountId = INCOMING_ACCOUNT_ID.withValue(i + "");
      final AccountId outgoingAccountId = OUTGOING_ACCOUNT_ID.withValue(i + "");

      final NextHopInfo nextHopInfo = NextHopInfo.builder()
        .nextHopAccountId(outgoingAccountId)
        .nextHopPacket(PREPARE_PACKET)
        .build();

      when(accountSettingsEntityMock.getAccountId()).thenReturn(incomingAccountId);
      when(accountSettingsRepositoryMock.findByAccountId(any())).thenReturn(Optional.of(accountSettingsEntityMock));
      when(nextHopPacketMapperMock.getNextHopPacket(eq(incomingAccountId), eq(PREPARE_PACKET))).thenReturn(nextHopInfo);
      // Re-use same link for all 5 outgoing accounts...
      when(linkManagerMock.getOrCreateLink(outgoingAccountId)).thenReturn(outgoingLink);

      final InterledgerResponsePacket actual = packetSwitch.switchPacket(incomingAccountId, PREPARE_PACKET);
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

      verify(linkManagerMock).getOrCreateLink(outgoingAccountId);
      verify(nextHopPacketMapperMock).getNextHopPacket(incomingAccountId, PREPARE_PACKET);
      verify(accountSettingsRepositoryMock).findByAccountId(incomingAccountId);
    }

    verify(packetSwitchFiltersMock, times(numReps)).size();
    verify(linkFiltersMock, times(numReps)).size();

    verifyZeroInteractions(connectorExceptionHandlerMock);
    verifyZeroInteractions(packetRejectorMock);

    verifyNoMoreInteractions(linkManagerMock);
    verifyNoMoreInteractions(nextHopPacketMapperMock);
    verifyNoMoreInteractions(accountSettingsRepositoryMock);
  }

}
