package org.interledger.connector.packetswitch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.interledger.core.InterledgerErrorCode.T00_INTERNAL_ERROR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.interledger.connector.ConnectorExceptionHandler;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.ImmutableAccountSettings;
import org.interledger.connector.caching.AccountSettingsLoadingCache;
import org.interledger.connector.events.PacketEventPublisher;
import org.interledger.connector.links.LinkManager;
import org.interledger.connector.links.NextHopInfo;
import org.interledger.connector.links.NextHopPacketMapper;
import org.interledger.connector.links.filters.LinkFilter;
import org.interledger.connector.packetswitch.filters.PacketSwitchFilter;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.link.Link;
import org.interledger.link.LinkId;
import org.interledger.link.LinkSettings;
import org.interledger.link.LoopbackLink;
import org.interledger.link.PacketRejector;

import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

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
    .amount(UnsignedLong.ONE)
    .expiresAt(Instant.now().plusSeconds(30))
    .executionCondition(InterledgerCondition.of(new byte[32]))
    .build();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

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
  private AccountSettingsLoadingCache accountSettingsLoadingCacheMock;
  @Mock
  private PacketRejector packetRejectorMock;
  @Mock
  private PacketEventPublisher packetEventPublisher;

  private Link outgoingLink;

  private DefaultILPv4PacketSwitch packetSwitch;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    this.outgoingLink = new LoopbackLink(
      () -> OPERATOR_ADDRESS,
      OUTGOING_LINK_SETTINGS,
      new PacketRejector(() -> OPERATOR_ADDRESS)
    );

    this.packetSwitch = new DefaultILPv4PacketSwitch(
      packetSwitchFiltersMock,
      linkFiltersMock,
      linkManagerMock,
      nextHopPacketMapperMock,
      connectorExceptionHandlerMock,
      packetRejectorMock,
      accountSettingsLoadingCacheMock,
      packetEventPublisher);
  }

  /**
   * Validate the PacketSwitch when the supplied account does not exist.
   */
  @Test
  public void switchPacketWithNoAccount() {
    final AccountId NON_EXISTENT_ACCOUNT_ID = AccountId.of("123");
    final LinkId NON_EXISTENT_LINK_ID = LinkId.of(NON_EXISTENT_ACCOUNT_ID.value());

    when(accountSettingsLoadingCacheMock.getAccount(any())).thenReturn(Optional.empty());

    final InterledgerRejectPacket rejectPacket = InterledgerRejectPacket.builder()
      .code(T00_INTERNAL_ERROR)
      .message("")
      .build();
    when(packetRejectorMock.reject(any(), any(), any(), anyString())).thenReturn(rejectPacket);

    expectedException.expect(InterledgerProtocolException.class);
    try {
      packetSwitch.switchPacket(NON_EXISTENT_ACCOUNT_ID, PREPARE_PACKET);
      fail("Should have thrown an InterledgerProtocolException!");
    } catch (InterledgerProtocolException e) {
      verify(packetRejectorMock).reject(
        eq(NON_EXISTENT_LINK_ID),
        eq(PREPARE_PACKET),
        eq(T00_INTERNAL_ERROR),
        eq("No Account found: `123`")
      );

      assertThat(e.getInterledgerRejectPacket()).isEqualTo(rejectPacket);

      verify(accountSettingsLoadingCacheMock).getAccount(eq(NON_EXISTENT_ACCOUNT_ID));
      verifyNoInteractions(connectorExceptionHandlerMock);
      verifyNoInteractions(nextHopPacketMapperMock);
      verifyNoInteractions(linkFiltersMock);
      verifyNoInteractions(linkManagerMock);
      verifyNoInteractions(packetSwitchFiltersMock);
      verifyNoMoreInteractions(packetRejectorMock);

      throw e;
    }
  }

  /**
   * Validate the PacketSwitch with the same account multiple times, and assert that the Cache is engaged (i.e.,
   * AccountSettingsRepository is engaged only once despite five packets).
   */
  @Test
  public void switchPacketMultipleTimeWithSameAccount() {
    final ImmutableAccountSettings incomingAccountSettings = AccountSettings.builder()
      .accountId(INCOMING_ACCOUNT_ID)
      .accountRelationship(AccountRelationship.PEER)
      .assetCode("USD")
      .assetScale(2)
      .linkType(LoopbackLink.LINK_TYPE)
      .build();
    when(accountSettingsLoadingCacheMock.getAccount(INCOMING_ACCOUNT_ID))
      .thenReturn(Optional.of(incomingAccountSettings));

    final ImmutableAccountSettings outgoingAccountSettings = AccountSettings.builder()
      .accountId(OUTGOING_ACCOUNT_ID)
      .accountRelationship(AccountRelationship.PEER)
      .assetCode("USD")
      .assetScale(2)
      .linkType(LoopbackLink.LINK_TYPE)
      .build();
    when(accountSettingsLoadingCacheMock.getAccount(OUTGOING_ACCOUNT_ID))
      .thenReturn(Optional.of(outgoingAccountSettings));

    // No FX/expiry changes, to keep things simple.
    final NextHopInfo nextHopInfo = NextHopInfo.builder()
      .nextHopAccountId(OUTGOING_ACCOUNT_ID)
      .nextHopPacket(PREPARE_PACKET)
      .build();
    when(nextHopPacketMapperMock.getNextHopPacket(eq(incomingAccountSettings), eq(PREPARE_PACKET)))
      .thenReturn(nextHopInfo);
    when(linkManagerMock.getOrCreateLink(OUTGOING_ACCOUNT_ID)).thenReturn(outgoingLink);
    when(nextHopPacketMapperMock.determineExchangeRate(any(), any(), any())).thenReturn(BigDecimal.ZERO);

    // Do the send numReps times to prove that the cache is working...
    final int numReps = 5;
    for (int i = 0; i < numReps; i++) {
      packetSwitch.switchPacket(INCOMING_ACCOUNT_ID, PREPARE_PACKET).handle(
        fulfillPacket -> assertThat(fulfillPacket.getFulfillment()).isEqualTo(LoopbackLink.LOOPBACK_FULFILLMENT),
        rejectPacket -> fail("Should have fulfilled but rejected!")
      );
    }

    verify(packetSwitchFiltersMock, times(numReps)).size();
    verify(linkFiltersMock, times(numReps)).size();
    verify(linkManagerMock, times(numReps)).getOrCreateLink(OUTGOING_ACCOUNT_ID);
    verify(nextHopPacketMapperMock, times(numReps)).getNextHopPacket(incomingAccountSettings, PREPARE_PACKET);
    verify(accountSettingsLoadingCacheMock, times(10)).getAccount(any());
    verify(nextHopPacketMapperMock, times(numReps)).determineExchangeRate(any(), any(), any());

    verifyNoInteractions(connectorExceptionHandlerMock);
    verifyNoInteractions(packetRejectorMock);
    verifyNoMoreInteractions(packetSwitchFiltersMock);
    verifyNoMoreInteractions(nextHopPacketMapperMock);
    verifyNoMoreInteractions(linkFiltersMock);
    verifyNoMoreInteractions(accountSettingsLoadingCacheMock);
  }

  /**
   * Validate the PacketSwitch with the five different accounts and assert that the Cache is not engaged (i.e.,
   * AccountSettingsRepository is engaged five times).
   */
  @Test
  public void switchPacketMultipleTimeWithDifferentAccounts() {
    final int numReps = 5;
    for (int i = 0; i < numReps; i++) {

      final AccountId incomingAccountID = INCOMING_ACCOUNT_ID.withValue(i + "");
      final AccountId outgoingAccountID = OUTGOING_ACCOUNT_ID.withValue(i + "");

      final ImmutableAccountSettings incomingAccountSettings =
        AccountSettings.builder()
          .accountId(incomingAccountID)
          .accountRelationship(AccountRelationship.PEER)
          .assetCode("USD")
          .assetScale(2)
          .linkType(LoopbackLink.LINK_TYPE)
          .build();

      final NextHopInfo nextHopInfo = NextHopInfo.builder()
        .nextHopAccountId(outgoingAccountID)
        .nextHopPacket(PREPARE_PACKET)
        .build();

      when(accountSettingsLoadingCacheMock.getAccount(any())).thenReturn(Optional.of(incomingAccountSettings));
      when(nextHopPacketMapperMock.getNextHopPacket(eq(incomingAccountSettings), eq(PREPARE_PACKET)))
        .thenReturn(nextHopInfo);
      when(linkManagerMock.getOrCreateLink(outgoingAccountID)).thenReturn(outgoingLink);

      when(nextHopPacketMapperMock.determineExchangeRate(any(), any(), any())).thenReturn(BigDecimal.ZERO);

      packetSwitch.switchPacket(incomingAccountID, PREPARE_PACKET).handle(
        fulfillPacket -> assertThat(fulfillPacket.getFulfillment()).isEqualTo(LoopbackLink.LOOPBACK_FULFILLMENT),
        rejectPacket -> fail("Should have fulfilled but rejected!")
      );

      verify(linkManagerMock).getOrCreateLink(eq(incomingAccountID));
      verify(linkManagerMock).getOrCreateLink(eq(outgoingAccountID));
      verify(nextHopPacketMapperMock).getNextHopPacket(incomingAccountSettings, PREPARE_PACKET);
      verify(accountSettingsLoadingCacheMock, times(2)).getAccount(incomingAccountID);
      verify(accountSettingsLoadingCacheMock, times(2)).getAccount(outgoingAccountID);
      verify(nextHopPacketMapperMock).determineExchangeRate(eq(incomingAccountSettings), any(), eq(PREPARE_PACKET));
    }

    verify(packetSwitchFiltersMock, times(numReps)).size();
    verify(linkFiltersMock, times(numReps)).size();

    verifyNoInteractions(connectorExceptionHandlerMock);
    verifyNoInteractions(packetRejectorMock);

    verifyNoMoreInteractions(linkManagerMock);
    verifyNoMoreInteractions(nextHopPacketMapperMock);
    verifyNoMoreInteractions(accountSettingsLoadingCacheMock);
  }

}
