package org.interledger.connector.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.connector.accounts.sub.LocalDestinationAddressUtils.PING_ACCOUNT_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.sub.LocalDestinationAddressUtils;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.core.InterledgerAddress;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

/**
 * Unit tests for {@link LocalDestinationAddressPaymentRouter}.
 */
public class LocalDestinationAddressPaymentRouterTest {

  private static final InterledgerAddress DESTINATION_ADDRESS = InterledgerAddress.of("test.foo");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  ConnectorSettings connectorSettingsMock;

  @Mock
  LocalDestinationAddressUtils localDestinationAddressUtilsMock;

  private LocalDestinationAddressPaymentRouter localDestinationAddressPaymentRouter;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(localDestinationAddressUtilsMock.getConnectorPingAccountId()).thenReturn(PING_ACCOUNT_ID);
    this.localDestinationAddressPaymentRouter
      = new LocalDestinationAddressPaymentRouter(() -> connectorSettingsMock, localDestinationAddressUtilsMock);
  }

  @Test
  public void findBestNexHopWithNullAddress() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("finalDestinationAddress must not be null!");

    localDestinationAddressPaymentRouter.findBestNexHop(null);
  }

  @Test
  public void findBestNexHopWhenNotLocalAddress() {
    when(localDestinationAddressUtilsMock.isLocalDestinationAddress(any())).thenReturn(false);

    assertThat(localDestinationAddressPaymentRouter.findBestNexHop(DESTINATION_ADDRESS)).isEmpty();

    verify(localDestinationAddressUtilsMock).getConnectorPingAccountId();
    verify(localDestinationAddressUtilsMock).isLocalDestinationAddress(any());
    verifyNoMoreInteractions(localDestinationAddressUtilsMock);
  }

  @Test
  public void findBestNexHopWhenLocalWithPingDisabled() {
    when(localDestinationAddressUtilsMock.isLocalDestinationAddress(any())).thenReturn(true);
    when(connectorSettingsMock.enabledProtocols().isPingProtocolEnabled()).thenReturn(false);

    assertThat(localDestinationAddressPaymentRouter.findBestNexHop(DESTINATION_ADDRESS)).isEmpty();

    verify(localDestinationAddressUtilsMock).getConnectorPingAccountId();
    verify(localDestinationAddressUtilsMock).isLocalDestinationAddress(any());
    verify(localDestinationAddressUtilsMock).isLocalSpspDestinationAddress(any());
    verify(localDestinationAddressUtilsMock).isLocalAccountDestinationAddress(any());
    verifyNoMoreInteractions(localDestinationAddressUtilsMock);
  }

  @Test
  public void findBestNexHopWhenLocalWithPingEnabledNotPing() {
    when(localDestinationAddressUtilsMock.isLocalDestinationAddress(any())).thenReturn(true);
    when(connectorSettingsMock.enabledProtocols().isPingProtocolEnabled()).thenReturn(true);
    when(localDestinationAddressUtilsMock.isAddressForConnectorPingAccount(any())).thenReturn(false);

    assertThat(localDestinationAddressPaymentRouter.findBestNexHop(DESTINATION_ADDRESS)).isEmpty();

    verify(localDestinationAddressUtilsMock).getConnectorPingAccountId();
    verify(localDestinationAddressUtilsMock).isLocalDestinationAddress(any());
    verify(localDestinationAddressUtilsMock).isAddressForConnectorPingAccount(any());
    verifyNoMoreInteractions(localDestinationAddressUtilsMock);
  }

  @Test
  public void findBestNexHopWhenLocalWithPingEnabledAddressIsPing() {
    when(localDestinationAddressUtilsMock.isLocalDestinationAddress(any())).thenReturn(true);
    when(connectorSettingsMock.enabledProtocols().isPingProtocolEnabled()).thenReturn(true);
    when(localDestinationAddressUtilsMock.isAddressForConnectorPingAccount(any())).thenReturn(true);

    final Optional<Route> actual = localDestinationAddressPaymentRouter.findBestNexHop(DESTINATION_ADDRESS);

    assertThat(actual).isNotEmpty();
    assertThat(actual.get().nextHopAccountId()).isEqualTo(PING_ACCOUNT_ID);

    verify(localDestinationAddressUtilsMock).getConnectorPingAccountId();
    verify(localDestinationAddressUtilsMock).isLocalDestinationAddress(any());
    verify(localDestinationAddressUtilsMock).isAddressForConnectorPingAccount(any());
    verifyNoMoreInteractions(localDestinationAddressUtilsMock);
  }

  @Test
  public void findBestNexHopWhenLocalSpspAddress() {
    final AccountId accountId = AccountId.of("foo");
    when(localDestinationAddressUtilsMock.isLocalDestinationAddress(any())).thenReturn(true);
    when(connectorSettingsMock.enabledProtocols().isPingProtocolEnabled()).thenReturn(false);
    when(localDestinationAddressUtilsMock.isAddressForConnectorPingAccount(any())).thenReturn(false);
    when(localDestinationAddressUtilsMock.isLocalSpspDestinationAddress(any())).thenReturn(true);
    when(localDestinationAddressUtilsMock.parseSpspAccountId(any())).thenReturn(accountId);

    final Optional<Route> actual = localDestinationAddressPaymentRouter.findBestNexHop(DESTINATION_ADDRESS);

    assertThat(actual).isNotEmpty();
    assertThat(actual.get().nextHopAccountId()).isEqualTo(accountId);

    verify(localDestinationAddressUtilsMock).getConnectorPingAccountId();
    verify(localDestinationAddressUtilsMock).isLocalDestinationAddress(any());
    verify(localDestinationAddressUtilsMock).isLocalSpspDestinationAddress(any());
    verify(localDestinationAddressUtilsMock).parseSpspAccountId(any());
    verifyNoMoreInteractions(localDestinationAddressUtilsMock);
  }

  @Test
  public void findBestNexHopWhenLocalAccountAddress() {
    final AccountId accountId = AccountId.of("foo");
    when(localDestinationAddressUtilsMock.isLocalDestinationAddress(any())).thenReturn(true);
    when(connectorSettingsMock.enabledProtocols().isPingProtocolEnabled()).thenReturn(false);
    when(localDestinationAddressUtilsMock.isAddressForConnectorPingAccount(any())).thenReturn(false);
    when(localDestinationAddressUtilsMock.isLocalSpspDestinationAddress(any())).thenReturn(false);
    when(localDestinationAddressUtilsMock.isLocalAccountDestinationAddress(any())).thenReturn(true);
    when(localDestinationAddressUtilsMock.parseLocalAccountId(any())).thenReturn(accountId);

    final Optional<Route> actual = localDestinationAddressPaymentRouter.findBestNexHop(DESTINATION_ADDRESS);

    assertThat(actual).isNotEmpty();
    assertThat(actual.get().nextHopAccountId()).isEqualTo(accountId);

    verify(localDestinationAddressUtilsMock).getConnectorPingAccountId();
    verify(localDestinationAddressUtilsMock).isLocalDestinationAddress(any());
    verify(localDestinationAddressUtilsMock).isLocalSpspDestinationAddress(any());
    verify(localDestinationAddressUtilsMock).isLocalAccountDestinationAddress(any());
    verify(localDestinationAddressUtilsMock).parseLocalAccountId(any());
    verifyNoMoreInteractions(localDestinationAddressUtilsMock);
  }

  @Test
  public void findBestNexHopWhenLocalDestinationButNoSubMatches() {
    final AccountId accountId = AccountId.of("foo");
    when(localDestinationAddressUtilsMock.isLocalDestinationAddress(any())).thenReturn(true);
    when(connectorSettingsMock.enabledProtocols().isPingProtocolEnabled()).thenReturn(false);
    when(localDestinationAddressUtilsMock.isAddressForConnectorPingAccount(any())).thenReturn(false);
    when(localDestinationAddressUtilsMock.isLocalSpspDestinationAddress(any())).thenReturn(false);
    when(localDestinationAddressUtilsMock.isLocalAccountDestinationAddress(any())).thenReturn(false);

    assertThat(localDestinationAddressPaymentRouter.findBestNexHop(DESTINATION_ADDRESS)).isEmpty();

    verify(localDestinationAddressUtilsMock).getConnectorPingAccountId();
    verify(localDestinationAddressUtilsMock).isLocalDestinationAddress(any());
    verify(localDestinationAddressUtilsMock).isLocalSpspDestinationAddress(any());
    verify(localDestinationAddressUtilsMock).isLocalAccountDestinationAddress(any());
    verifyNoMoreInteractions(localDestinationAddressUtilsMock);
  }
}
