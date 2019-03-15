package com.sappenin.interledger.ilpv4.connector.packetswitch.filters;

import com.sappenin.interledger.ilpv4.connector.accounts.AccountManager;
import com.sappenin.interledger.ilpv4.connector.ccp.CcpRouteControlRequest;
import com.sappenin.interledger.ilpv4.connector.ccp.CcpSyncMode;
import com.sappenin.interledger.ilpv4.connector.ccp.codecs.CcpCodecContextFactory;
import com.sappenin.interledger.ilpv4.connector.routing.CcpSender;
import com.sappenin.interledger.ilpv4.connector.routing.ExternalRoutingService;
import com.sappenin.interledger.ilpv4.connector.routing.RoutableAccount;
import com.sappenin.interledger.ilpv4.connector.routing.RoutingTableId;
import com.sappenin.interledger.ilpv4.connector.settings.EnabledProtocolSettings;
import org.interledger.connector.accounts.Account;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.core.InterledgerResponsePacketHandler;
import org.interledger.ildcp.IldcpResponsePacket;
import org.interledger.ildcp.asn.framework.IldcpCodecContextFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.sappenin.interledger.ilpv4.connector.ccp.CcpConstants.CCP_CONTROL_DESTINATION_ADDRESS;
import static com.sappenin.interledger.ilpv4.connector.ccp.CcpConstants.PEER_PROTOCOL_EXECUTION_CONDITION;
import static com.sappenin.interledger.ilpv4.connector.ccp.CcpConstants.PEER_PROTOCOL_EXECUTION_FULFILLMENT;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PeerProtocolPacketFilter}.
 */
public class PeerProtocolPacketFilterTest {

  private static final AccountId ACCOUNT_ID = AccountId.of("test-account");
  private static final InterledgerAddress OPERATOR_ADDRESS = InterledgerAddress.of("example.foo");

  @Mock
  private EnabledProtocolSettings enabledProtocolSettingsMock;

  @Mock
  private PacketSwitchFilterChain filterChainMock;

  @Mock
  private ExternalRoutingService externalRoutingServiceMock;

  @Mock
  private AccountManager accountManagerMock;

  private PeerProtocolPacketFilter filter;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    this.filter = new PeerProtocolPacketFilter(
      () -> OPERATOR_ADDRESS,
      enabledProtocolSettingsMock,
      externalRoutingServiceMock,
      accountManagerMock,
      CcpCodecContextFactory.oer(),
      IldcpCodecContextFactory.oer()
    );

    final AccountSettings accountSettingsMock = mock(AccountSettings.class);
    when(accountSettingsMock.getAssetScale()).thenReturn(9);
    when(accountSettingsMock.getAssetCode()).thenReturn("XRP");

    final Account account = mock(Account.class);
    when(account.getId()).thenReturn(ACCOUNT_ID);
    when(account.getAccountSettings()).thenReturn(accountSettingsMock);

    when(accountManagerMock.getAccount(ACCOUNT_ID)).thenReturn(Optional.of(account));
    when(accountManagerMock.toChildAddress(ACCOUNT_ID)).thenReturn(OPERATOR_ADDRESS);

    final RoutableAccount routableAccountMock = mock(RoutableAccount.class);
    when(routableAccountMock.getCcpSender()).thenReturn(mock(CcpSender.class));
    when(externalRoutingServiceMock.getTrackedAccount(ACCOUNT_ID)).thenReturn(Optional.of(routableAccountMock));
  }

  @Test(expected = NullPointerException.class)
  public void doFilterWithNullPacket() {
    try {
      filter.doFilter(ACCOUNT_ID, null, filterChainMock);
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
    final InterledgerPreparePacket preparePacket = this.constructPreparePacket(InterledgerAddress.of("peer.config"));

    final InterledgerResponsePacket result = filter.doFilter(ACCOUNT_ID, preparePacket, filterChainMock);

    new InterledgerResponsePacketHandler() {

      @Override
      protected void handleFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
        fail(String.format("Should not fulfill: %s", interledgerFulfillPacket));
      }

      @Override
      protected void handleRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
        assertThat(interledgerRejectPacket.getCode(), is(InterledgerErrorCode.F00_BAD_REQUEST));
        assertThat(interledgerRejectPacket.getMessage(), is("IL-DCP is not supported by this node."));
      }
    }.handle(result);
  }

  @Test
  public void doFilterForPeerDotConfigWhenEnabledWithNoAccount() {
    when(enabledProtocolSettingsMock.isPeerConfigEnabled()).thenReturn(true);
    final InterledgerPreparePacket preparePacket = this.constructPreparePacket(InterledgerAddress.of("peer.config"));

    final InterledgerResponsePacket result = filter.doFilter(AccountId.of("foo"), preparePacket, filterChainMock);

    new InterledgerResponsePacketHandler() {

      @Override
      protected void handleFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
        fail(String.format("Should not fulfill: %s", interledgerFulfillPacket));
      }

      @Override
      protected void handleRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
        assertThat(interledgerRejectPacket.getCode(), is(InterledgerErrorCode.F00_BAD_REQUEST));
        assertThat(interledgerRejectPacket.getMessage(), is("Invalid Source Account: `foo`"));
      }
    }.handle(result);
  }

  @Test
  public void doFilterForPeerDotConfigWhenEnabled() {
    when(enabledProtocolSettingsMock.isPeerConfigEnabled()).thenReturn(true);
    final InterledgerPreparePacket preparePacket = this.constructPreparePacket(InterledgerAddress.of("peer.config"));

    final InterledgerResponsePacket result = filter.doFilter(ACCOUNT_ID, preparePacket, filterChainMock);

    new InterledgerResponsePacketHandler() {

      @Override
      protected void handleFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
        assertThat(interledgerFulfillPacket.getFulfillment(), is(IldcpResponsePacket.EXECUTION_FULFILLMENT));
      }

      @Override
      protected void handleRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
        fail(String.format("Should not reject: %s", interledgerRejectPacket));
      }
    }.handle(result);
  }

  ////////////////
  // `peer.route`
  ////////////////

  @Test
  public void doFilterForPeerDotRouteWhenDisabled() {
    when(enabledProtocolSettingsMock.isPeerRoutingEnabled()).thenReturn(false);
    final InterledgerPreparePacket preparePacket = this.constructPreparePacket(InterledgerAddress.of("peer.route"));

    final InterledgerResponsePacket result = filter.doFilter(ACCOUNT_ID, preparePacket, filterChainMock);

    new InterledgerResponsePacketHandler() {

      @Override
      protected void handleFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
        fail(String.format("Should not fulfill: %s", interledgerFulfillPacket));
      }

      @Override
      protected void handleRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
        assertThat(interledgerRejectPacket.getCode(), is(InterledgerErrorCode.F00_BAD_REQUEST));
        assertThat(interledgerRejectPacket.getMessage(), is("CCP routing protocol is not supported by this node."));
      }
    }.handle(result);
  }

  @Test
  public void doFilterForPeerDotRouteWhenEnabled() throws IOException {
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
      .amount(BigInteger.TEN)
      .expiresAt(Instant.now().plusSeconds(30))
      .data(os.toByteArray())
      .build();

    final InterledgerResponsePacket result = filter.doFilter(ACCOUNT_ID, preparePacket, filterChainMock);

    new InterledgerResponsePacketHandler() {

      @Override
      protected void handleFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
        assertThat(interledgerFulfillPacket.getFulfillment(), is(PEER_PROTOCOL_EXECUTION_FULFILLMENT));
      }

      @Override
      protected void handleRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
        fail(String.format("Should not reject: %s", interledgerRejectPacket));
      }
    }.handle(result);
  }

  ////////////////
  // `peer.foo`
  ////////////////

  @Test
  public void doFilterForPeerDotFoo() {
    final InterledgerPreparePacket preparePacket = this.constructPreparePacket(InterledgerAddress.of("peer.foo"));

    final InterledgerResponsePacket result = filter.doFilter(ACCOUNT_ID, preparePacket, filterChainMock);

    new InterledgerResponsePacketHandler() {

      @Override
      protected void handleFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
        fail(String.format("Should not fulfill: %s", interledgerFulfillPacket));
      }

      @Override
      protected void handleRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
        assertThat(interledgerRejectPacket.getCode(), is(InterledgerErrorCode.F01_INVALID_PACKET));
        assertThat(interledgerRejectPacket.getMessage(), is("unknown peer protocol."));
      }
    }.handle(result);
  }

  ///////////////
  // Helpers
  ////////////////

  private InterledgerPreparePacket constructPreparePacket(final InterledgerAddress destinationAddress) {
    Objects.requireNonNull(destinationAddress);
    return InterledgerPreparePacket.builder()
      .destination(destinationAddress)
      .executionCondition(InterledgerCondition.of(new byte[32]))
      .amount(BigInteger.TEN)
      .expiresAt(Instant.now().plusSeconds(30))
      .build();
  }
}