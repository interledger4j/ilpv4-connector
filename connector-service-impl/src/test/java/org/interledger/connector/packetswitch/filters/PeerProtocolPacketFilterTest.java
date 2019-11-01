package org.interledger.connector.packetswitch.filters;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.interledger.connector.ccp.CcpConstants.CCP_CONTROL_DESTINATION_ADDRESS;
import static org.interledger.connector.ccp.CcpConstants.CCP_UPDATE_DESTINATION_ADDRESS;
import static org.interledger.connector.ccp.CcpConstants.PEER_PROTOCOL_EXECUTION_CONDITION;
import static org.interledger.connector.ccp.CcpConstants.PEER_PROTOCOL_EXECUTION_FULFILLMENT;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.interledger.codecs.ildcp.IldcpCodecContextFactory;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.ccp.CcpRouteControlRequest;
import org.interledger.connector.ccp.CcpRouteUpdateRequest;
import org.interledger.connector.ccp.CcpSyncMode;
import org.interledger.connector.ccp.codecs.CcpCodecContextFactory;
import org.interledger.connector.routing.CcpReceiver;
import org.interledger.connector.routing.CcpSender;
import org.interledger.connector.routing.RoutableAccount;
import org.interledger.connector.routing.RouteBroadcaster;
import org.interledger.connector.routing.RoutingTableId;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.connector.settings.EnabledProtocolSettings;
import org.interledger.connector.settlement.SettlementService;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.ildcp.IldcpRequestPacket;
import org.interledger.ildcp.IldcpResponsePacket;
import org.interledger.link.PacketRejector;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Unit tests for {@link PeerProtocolPacketFilter}.
 */
@RunWith(Parameterized.class)
public class PeerProtocolPacketFilterTest {

  private static final boolean SEND_ROUTES_ENABLED = true;
  private static final boolean SEND_ROUTES_DISABLED = false;
  private static final boolean RECEIVE_ROUTES_ENABLED = true;
  private static final boolean RECEIVE_ROUTES_DISABLED = false;

  private static final AccountId ACCOUNT_ID = AccountId.of("test-account");
  private static final InterledgerAddress OPERATOR_ADDRESS = InterledgerAddress.of("example.foo");

  private static final InterledgerRejectPacket REJECT_PACKET = InterledgerRejectPacket.builder()
      .triggeredBy(OPERATOR_ADDRESS)
      .code(InterledgerErrorCode.F00_BAD_REQUEST)
      .message("error message")
      .build();

  @Mock
  RouteBroadcaster routeBroadcasterMock;
  @Mock
  ConnectorSettings connectorSettingsMock;
  @Mock
  EnabledProtocolSettings enabledProtocolSettingsMock;
  @Mock
  PacketRejector packetRejectorMock;
  @Mock
  AccountSettings accountSettingsMock;
  @Mock
  PacketSwitchFilterChain filterChainMock;
  @Mock
  SettlementService settlementService;

  private boolean sendRoutesEnabled;
  private boolean receiveRoutesEnabled;
  private PeerProtocolPacketFilter filter;

  public PeerProtocolPacketFilterTest(final boolean sendRoutesEnabled, final boolean receiveRoutesEnabled) {
    this.sendRoutesEnabled = sendRoutesEnabled;
    this.receiveRoutesEnabled = receiveRoutesEnabled;
  }

  @Parameterized.Parameters
  public static Collection<Object[]> errorCodes() {
    return ImmutableList.of(
        // Both disabled
        new Object[] {SEND_ROUTES_DISABLED, RECEIVE_ROUTES_DISABLED},
        // send enabled
        new Object[] {SEND_ROUTES_ENABLED, RECEIVE_ROUTES_DISABLED},
        // receive enabled
        new Object[] {SEND_ROUTES_DISABLED, RECEIVE_ROUTES_ENABLED},
        // Both enabled
        new Object[] {SEND_ROUTES_ENABLED, RECEIVE_ROUTES_ENABLED}
    );
  }

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(connectorSettingsMock.enabledProtocols()).thenReturn(enabledProtocolSettingsMock);

    when(packetRejectorMock.reject(any(), any(), any(), any())).thenReturn(REJECT_PACKET);

    this.filter = new PeerProtocolPacketFilter(
        () -> connectorSettingsMock,
        packetRejectorMock,
        routeBroadcasterMock,
        CcpCodecContextFactory.oer(),
        IldcpCodecContextFactory.oer(),
        settlementService
    );

    when(accountSettingsMock.accountId()).thenReturn(ACCOUNT_ID);
    when(accountSettingsMock.assetScale()).thenReturn(9);
    when(accountSettingsMock.assetCode()).thenReturn("XRP");
    when(accountSettingsMock.isSendRoutes()).thenReturn(sendRoutesEnabled);
    when(accountSettingsMock.isReceiveRoutes()).thenReturn(receiveRoutesEnabled);

    when(connectorSettingsMock.toChildAddress(ACCOUNT_ID)).thenReturn(OPERATOR_ADDRESS);

    final RoutableAccount routableAccountMock = mock(RoutableAccount.class);
    when(routableAccountMock.ccpSender()).thenReturn(mock(CcpSender.class));
    when(routableAccountMock.ccpReceiver()).thenReturn(mock(CcpReceiver.class));
    when(routeBroadcasterMock.getCcpEnabledAccount(ACCOUNT_ID)).thenReturn(Optional.of(routableAccountMock));
  }

  @Test(expected = NullPointerException.class)
  public void doFilterWithNullPacket() {
    try {
      filter.doFilter(accountSettingsMock, null, filterChainMock);
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is(nullValue()));
      throw e;
    }
  }

  ////////////////
  // `peer.config`
  ////////////////

  @Test
  public void doFilterForPeerDotConfigWhenDisabled() {
    when(enabledProtocolSettingsMock.isPeerConfigEnabled()).thenReturn(false);
    final InterledgerPreparePacket preparePacket = this.constructPreparePacket(IldcpRequestPacket.PEER_DOT_CONFIG);

    filter.doFilter(accountSettingsMock, preparePacket, filterChainMock).handle(
        fulfillPacket -> fail(String.format("Should not fulfill: %s", fulfillPacket)),
        rejectPacket -> {
          final ArgumentCaptor<InterledgerErrorCode> errorCodeArgumentCaptor =
              ArgumentCaptor.forClass(InterledgerErrorCode.class);
          final ArgumentCaptor<String> errorMessageCaptor = ArgumentCaptor.forClass(String.class);
          verify(packetRejectorMock)
              .reject(any(), any(), errorCodeArgumentCaptor.capture(), errorMessageCaptor.capture());

          assertThat(errorCodeArgumentCaptor.getValue(), is(InterledgerErrorCode.F00_BAD_REQUEST));
          assertThat(errorMessageCaptor.getValue(), is("IL-DCP is not supported by this Connector."));
        }
    );
  }

  /**
   * Uses the DataProvider to test various settings...
   */
  @Test
  public void doFilterForPeerDotConfigWithVariousSettings() {
    when(enabledProtocolSettingsMock.isPeerConfigEnabled()).thenReturn(true);
    final InterledgerPreparePacket preparePacket = this.constructPreparePacket(IldcpRequestPacket.PEER_DOT_CONFIG);

    filter.doFilter(accountSettingsMock, preparePacket, filterChainMock)
        .handle(
            fulfillPacket -> assertThat(fulfillPacket.getFulfillment(), is(IldcpResponsePacket.EXECUTION_FULFILLMENT)),
            rejectPacket -> fail(String.format("Should not reject: %s", rejectPacket))
        );
  }

  ////////////////
  // `peer.route`
  ////////////////

  /**
   * Uses the DataProvider to test various settings...
   */
  @Test
  public void doFilterForPeerDotRouteWhenDisabled() {
    when(enabledProtocolSettingsMock.isPeerRoutingEnabled()).thenReturn(false);
    final InterledgerPreparePacket preparePacket = this.constructPreparePacket(InterledgerAddress.of("peer.route"));

    filter.doFilter(accountSettingsMock, preparePacket, filterChainMock).handle(
        fulfillPacket -> fail(String.format("Should not fulfill: %s", fulfillPacket)),
        rejectPacket -> {
          final ArgumentCaptor<InterledgerErrorCode> errorCodeArgumentCaptor =
              ArgumentCaptor.forClass(InterledgerErrorCode.class);
          final ArgumentCaptor<String> errorMessageCaptor = ArgumentCaptor.forClass(String.class);
          verify(packetRejectorMock)
              .reject(any(), any(), errorCodeArgumentCaptor.capture(), errorMessageCaptor.capture());

          assertThat(errorCodeArgumentCaptor.getValue(), is(InterledgerErrorCode.F00_BAD_REQUEST));
          assertThat(errorMessageCaptor.getValue(), is("CCP routing protocol is not supported by this node."));
        }
    );
  }

  /**
   * When Peer routing is enabled at the Connector level, the code falls-back to account-level checks. Because this is a
   * parameterized test, all potenital values of Account.send/receive will be tested.
   */
  @Test
  public void doFilterForPeerDotRouteDotControl() throws IOException {
    when(enabledProtocolSettingsMock.isPeerRoutingEnabled()).thenReturn(true);

    final CcpRouteControlRequest routeControlRequest = CcpRouteControlRequest.builder()
        .lastKnownEpoch(1)
        .lastKnownRoutingTableId(RoutingTableId.of(UUID.randomUUID()))
        .mode(CcpSyncMode.MODE_IDLE)
        .build();

    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    CcpCodecContextFactory.oer().write(routeControlRequest, os);

    final InterledgerPreparePacket preparePacket = InterledgerPreparePacket.builder()
        .destination(CCP_CONTROL_DESTINATION_ADDRESS)
        .executionCondition(PEER_PROTOCOL_EXECUTION_CONDITION)
        .amount(UnsignedLong.valueOf(10))
        .expiresAt(Instant.now().plusSeconds(30))
        .data(os.toByteArray())
        .build();

    filter.doFilter(accountSettingsMock, preparePacket, filterChainMock).handle(
        fulfillPacket -> {
          if (!sendRoutesEnabled) {
            fail(String.format("Should not have fulfilled when sendRoutes is disabled! %s", fulfillPacket));
          } else {
            assertThat("Should have fulfilled when sendRoutes is enabled",
                fulfillPacket.getFulfillment(), is(PEER_PROTOCOL_EXECUTION_FULFILLMENT));
          }
        },
        rejectPacket -> {
          // Route control messages have no connection to `receiveEnabled`.
          if (!sendRoutesEnabled) {
            final ArgumentCaptor<InterledgerErrorCode> errorCodeArgumentCaptor =
                ArgumentCaptor.forClass(InterledgerErrorCode.class);
            final ArgumentCaptor<String> errorMessageCaptor = ArgumentCaptor.forClass(String.class);
            verify(packetRejectorMock)
                .reject(any(), any(), errorCodeArgumentCaptor.capture(), errorMessageCaptor.capture());
            assertThat(errorCodeArgumentCaptor.getValue(), is(InterledgerErrorCode.F00_BAD_REQUEST));
            assertThat(errorMessageCaptor.getValue(),
                is("CCP sending is not enabled for this account. destinationAddress=peer.route.control"));

            assertThat(rejectPacket.getCode(), is(InterledgerErrorCode.F00_BAD_REQUEST));
            assertThat(rejectPacket.getTriggeredBy().get(), is(OPERATOR_ADDRESS));
          } else {
            fail(String.format("Should not have rejected when sendRoutes is enabled. %s", rejectPacket));
          }
        }
    );
  }

  /**
   * When Peer routing is enabled at the Connector level, the code falls-back to account-level checks. Because this is a
   * parameterized test, all potenital values of Account.send/receive will be tested.
   */
  @Test
  public void doFilterForPeerDotRouteDotUpdate() throws IOException {
    when(enabledProtocolSettingsMock.isPeerRoutingEnabled()).thenReturn(true);

    final CcpRouteUpdateRequest routeControlRequest = CcpRouteUpdateRequest.builder()
        .currentEpochIndex(0)
        .fromEpochIndex(0)
        .toEpochIndex(1)
        .holdDownTime(9L)
        .routingTableId(RoutingTableId.of(UUID.randomUUID()))
        .speaker(OPERATOR_ADDRESS)
        .build();

    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    CcpCodecContextFactory.oer().write(routeControlRequest, os);

    final InterledgerPreparePacket preparePacket = InterledgerPreparePacket.builder()
        .destination(CCP_UPDATE_DESTINATION_ADDRESS)
        .executionCondition(PEER_PROTOCOL_EXECUTION_CONDITION)
        .amount(UnsignedLong.valueOf(10))
        .expiresAt(Instant.now().plusSeconds(30))
        .data(os.toByteArray())
        .build();

    filter.doFilter(accountSettingsMock, preparePacket, filterChainMock).handle(
        fulfillPacket -> {
          if (!receiveRoutesEnabled) {
            fail(String.format("Should not have fulfilled when sendRoutes is disabled! %s", fulfillPacket));
          } else {
            assertThat("Should have fulfilled when receiveRoutes is enabled",
                fulfillPacket.getFulfillment(), is(PEER_PROTOCOL_EXECUTION_FULFILLMENT));
          }
        },
        rejectPacket -> {
          // Route control messages have no connection to `receiveEnabled`.
          if (!receiveRoutesEnabled) {
            final ArgumentCaptor<InterledgerErrorCode> errorCodeArgumentCaptor =
                ArgumentCaptor.forClass(InterledgerErrorCode.class);
            final ArgumentCaptor<String> errorMessageCaptor = ArgumentCaptor.forClass(String.class);
            verify(packetRejectorMock)
                .reject(any(), any(), errorCodeArgumentCaptor.capture(), errorMessageCaptor.capture());
            assertThat(errorCodeArgumentCaptor.getValue(), is(InterledgerErrorCode.F00_BAD_REQUEST));
            assertThat(errorMessageCaptor.getValue(),
                is("CCP receiving is not enabled for this account. destinationAddress=peer.route.update"));

            assertThat(rejectPacket.getCode(), is(InterledgerErrorCode.F00_BAD_REQUEST));
            assertThat(rejectPacket.getTriggeredBy().get(), is(OPERATOR_ADDRESS));
          } else {
            fail(String.format("Should not have rejected when receiveRoutes is enabled! %s", rejectPacket));
          }
        }
    );
  }

  ////////////////
  // `peer.foo`
  ////////////////

  @Test
  public void doFilterForPeerDotFoo() {
    final InterledgerPreparePacket preparePacket = this.constructPreparePacket(InterledgerAddress.of("peer.foo"));

    filter.doFilter(accountSettingsMock, preparePacket, filterChainMock).handle(
        fulfillPacket -> fail(String.format("Should not fulfill: %s", fulfillPacket)),
        rejectPacket -> {
          final ArgumentCaptor<InterledgerErrorCode> errorCodeArgumentCaptor =
              ArgumentCaptor.forClass(InterledgerErrorCode.class);
          final ArgumentCaptor<String> errorMessageCaptor = ArgumentCaptor.forClass(String.class);
          verify(packetRejectorMock)
              .reject(any(), any(), errorCodeArgumentCaptor.capture(), errorMessageCaptor.capture());

          assertThat(errorCodeArgumentCaptor.getValue(), is(InterledgerErrorCode.F01_INVALID_PACKET));
          assertThat(errorMessageCaptor.getValue(), is("unknown peer protocol."));
        }
    );
  }

  ///////////////
  // Helpers
  ////////////////

  private InterledgerPreparePacket constructPreparePacket(final InterledgerAddress destinationAddress) {
    Objects.requireNonNull(destinationAddress);
    return InterledgerPreparePacket.builder()
        .destination(destinationAddress)
        .executionCondition(InterledgerCondition.of(new byte[32]))
        .amount(UnsignedLong.valueOf(10))
        .expiresAt(Instant.now().plusSeconds(30))
        .build();
  }

  // TODO: UPDATE TEST FOR SETTLEMENT SERVICE FUNCTIONALITY!!!!!!!!!!!!!!!!!!!!!!!!!!!!


}
