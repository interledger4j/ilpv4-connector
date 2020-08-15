package org.interledger.connector.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.ccp.codecs.CcpCodecContextFactory;
import org.interledger.connector.links.LinkManager;
import org.interledger.connector.persistence.repositories.AccountSettingsRepository;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.connector.settings.ImmutableConnectorSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.link.Link;
import org.interledger.link.LoopbackLink;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.concurrent.Executors;

/**
 * Unit test for {@link DefaultRouteBroadcaster}.
 */
public class DefaultRouteBroadcasterTest {

  private static final AccountId ACCOUNT_ID_1 = AccountId.of("test-account-1");
  private static final AccountId ACCOUNT_ID_2 = AccountId.of("test-account-2");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private ConnectorSettings connectorSettings;

  @Mock
  private ForwardingRoutingTable<RouteUpdate> outgoingRoutingTableMock;
  @Mock
  private AccountSettingsRepository accountSettingsRepositoryMock;
  @Mock
  private LinkManager linkManagerMock;
  @Mock
  private Link link;

  private DefaultRouteBroadcaster defaultRouteBroadcaster;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    connectorSettings = ImmutableConnectorSettings.builder()
      .operatorAddress(InterledgerAddress.of("test.foo"))
      .build();

    when(link.sendPacket(any())).thenReturn(InterledgerRejectPacket.builder()
      .code(InterledgerErrorCode.F99_APPLICATION_ERROR)
      .message("test rejection packet")
      .build());

    final AccountSettings accountSettings1 = AccountSettings.builder()
      .accountId(ACCOUNT_ID_1)
      .assetCode("XRP")
      .assetScale(9)
      .description("Test Account 1")
      .accountRelationship(AccountRelationship.CHILD)
      .linkType(LoopbackLink.LINK_TYPE)
      .build();
    when(accountSettingsRepositoryMock.findByAccountIdWithConversion(eq(ACCOUNT_ID_1)))
      .thenReturn(Optional.of(accountSettings1));

    final AccountSettings accountSettings2 = AccountSettings.builder()
      .accountId(ACCOUNT_ID_2)
      .assetCode("USD")
      .assetScale(2)
      .description("Test Account 2")
      .accountRelationship(AccountRelationship.CHILD)
      .linkType(LoopbackLink.LINK_TYPE)
      .build();
    when(accountSettingsRepositoryMock.findByAccountIdWithConversion(eq(ACCOUNT_ID_2)))
      .thenReturn(Optional.of(accountSettings2));

    when(linkManagerMock.getOrCreateLink(Mockito.<AccountId>any())).thenReturn(link);

    this.defaultRouteBroadcaster = new DefaultRouteBroadcaster(
      () -> connectorSettings,
      CcpCodecContextFactory.oer(),
      outgoingRoutingTableMock,
      accountSettingsRepositoryMock,
      linkManagerMock,
      Executors.newSingleThreadExecutor()
    );
  }

  ////////////////////////////
  // registerCcpEnabledAccount
  ////////////////////////////

  @Test
  public void registerCcpEnabledAccountWithNullId() {
    expectedException.expect(NullPointerException.class);
    AccountId nullAccountId = null;
    defaultRouteBroadcaster.registerCcpEnabledAccount(nullAccountId);
  }

  // false / false
  @Test
  public void registerCcpEnabledAccountSendFalseReceiveFalse() {
    final AccountSettings accountSettings = accountSettings(false, false);
    when(accountSettingsRepositoryMock.findByAccountIdWithConversion(any()))
      .thenReturn(Optional.of(accountSettings));

    assertThat(defaultRouteBroadcaster.registerCcpEnabledAccount(ACCOUNT_ID_1)).isEmpty();
    Mockito.verifyNoInteractions(link); // Route control should not be initiated.
  }

  // true / false
  @Test
  public void registerCcpEnabledAccountSendTrueReceiveFalse() {
    final AccountSettings accountSettings = accountSettings(true, false);
    when(accountSettingsRepositoryMock.findByAccountIdWithConversion(any()))
      .thenReturn(Optional.of(accountSettings));

    final Optional<RoutableAccount> optRoutableAccount = defaultRouteBroadcaster
      .registerCcpEnabledAccount(ACCOUNT_ID_1);
    assertThat(optRoutableAccount).isNotEmpty();

    final RoutableAccount routableAccount = optRoutableAccount.get();
    assertThat(routableAccount.accountId()).isEqualTo(ACCOUNT_ID_1);
    Mockito.verifyNoInteractions(link); // Route control should not be initiated.
  }

  // false / true
  @Test
  public void registerCcpEnabledAccountSendFalseReceiveTrue() {
    final AccountSettings accountSettings = accountSettings(false, true);
    when(accountSettingsRepositoryMock.findByAccountIdWithConversion(Mockito.any()))
      .thenReturn(Optional.of(accountSettings));

    final Optional<RoutableAccount> optRoutableAccount = defaultRouteBroadcaster
      .registerCcpEnabledAccount(ACCOUNT_ID_1);
    assertThat(optRoutableAccount).isNotEmpty();

    final RoutableAccount routableAccount = optRoutableAccount.get();
    assertThat(routableAccount.accountId()).isEqualTo(ACCOUNT_ID_1);
    Mockito.verify(link).sendPacket(any()); // Route control should  be initiated.
    Mockito.verifyNoMoreInteractions(link);
  }

  // true / true
  @Test
  public void registerCcpEnabledAccountSendTrueReceiveTrue() {
    final AccountSettings accountSettings = accountSettings(true, true);
    when(accountSettingsRepositoryMock.findByAccountIdWithConversion(Mockito.any()))
      .thenReturn(Optional.of(accountSettings));

    final Optional<RoutableAccount> optRoutableAccount = defaultRouteBroadcaster
      .registerCcpEnabledAccount(ACCOUNT_ID_1);
    assertThat(optRoutableAccount).isNotEmpty();

    final RoutableAccount routableAccount = optRoutableAccount.get();
    assertThat(routableAccount.accountId()).isEqualTo(ACCOUNT_ID_1);
    Mockito.verify(link).sendPacket(any()); // Route control should  be initiated.
    Mockito.verifyNoMoreInteractions(link);
  }

  @Test
  public void registerCcpEnabledAccountTwiceWithReceiveTrue() {
    final AccountSettings accountSettings = accountSettings(true, true);
    when(accountSettingsRepositoryMock.findByAccountIdWithConversion(Mockito.any()))
      .thenReturn(Optional.of(accountSettings));

    Optional<RoutableAccount> optRoutableAccount = defaultRouteBroadcaster.registerCcpEnabledAccount(ACCOUNT_ID_1);
    assertThat(optRoutableAccount).isNotEmpty();

    optRoutableAccount = defaultRouteBroadcaster.registerCcpEnabledAccount(ACCOUNT_ID_1);
    assertThat(optRoutableAccount).isNotEmpty();

    final RoutableAccount routableAccount = optRoutableAccount.get();
    assertThat(routableAccount.accountId()).isEqualTo(ACCOUNT_ID_1);
    Mockito.verify(link, times(2)).sendPacket(any()); // Route control should  be initiated.
    Mockito.verifyNoMoreInteractions(link);
  }

  @Test
  public void registerCcpEnabledAccountTwiceWithReceiveFalse() {
    final AccountSettings accountSettings = accountSettings(true, false);
    when(accountSettingsRepositoryMock.findByAccountIdWithConversion(Mockito.any()))
      .thenReturn(Optional.of(accountSettings));

    Optional<RoutableAccount> optRoutableAccount = defaultRouteBroadcaster.registerCcpEnabledAccount(ACCOUNT_ID_1);
    assertThat(optRoutableAccount).isNotEmpty();

    optRoutableAccount = defaultRouteBroadcaster.registerCcpEnabledAccount(ACCOUNT_ID_1);
    assertThat(optRoutableAccount).isNotEmpty();

    final RoutableAccount routableAccount = optRoutableAccount.get();
    assertThat(routableAccount.accountId()).isEqualTo(ACCOUNT_ID_1);
    Mockito.verifyNoInteractions(link); // Route control should not be initiated.
  }

  @Test
  public void getCcpEnabledAccount() {
    assertThat(defaultRouteBroadcaster.getCcpEnabledAccount(ACCOUNT_ID_1)).isEmpty();

    Optional<RoutableAccount> optRoutableAccount = defaultRouteBroadcaster.registerCcpEnabledAccount(ACCOUNT_ID_1);
    assertThat(optRoutableAccount).isNotEmpty();

    assertThat(defaultRouteBroadcaster.getCcpEnabledAccount(ACCOUNT_ID_1)).isEqualTo(optRoutableAccount);
  }

  @Test
  public void getAllCcpEnabledAccounts() {
    assertThat(defaultRouteBroadcaster.getAllCcpEnabledAccounts()).isEmpty();

    defaultRouteBroadcaster.registerCcpEnabledAccount(ACCOUNT_ID_1);
    assertThat(defaultRouteBroadcaster.getAllCcpEnabledAccounts()).hasSize(1);

    defaultRouteBroadcaster.registerCcpEnabledAccount(ACCOUNT_ID_2);
    assertThat(defaultRouteBroadcaster.getAllCcpEnabledAccounts()).hasSize(2);

    defaultRouteBroadcaster.removeAccount(ACCOUNT_ID_1);
    assertThat(defaultRouteBroadcaster.getCcpEnabledAccount(ACCOUNT_ID_1)).isEmpty();
    assertThat(defaultRouteBroadcaster.getCcpEnabledAccount(ACCOUNT_ID_2)).isNotEmpty();
    assertThat(defaultRouteBroadcaster.getAllCcpEnabledAccounts()).hasSize(1);

    defaultRouteBroadcaster.removeAccount(ACCOUNT_ID_2);
    assertThat(defaultRouteBroadcaster.getCcpEnabledAccount(ACCOUNT_ID_1)).isEmpty();
    assertThat(defaultRouteBroadcaster.getCcpEnabledAccount(ACCOUNT_ID_2)).isEmpty();
    assertThat(defaultRouteBroadcaster.getAllCcpEnabledAccounts()).isEmpty();
  }

  private AccountSettings accountSettings(boolean sendRoutes, boolean receiveRoutes) {
    return AccountSettings.builder()
      .accountId(ACCOUNT_ID_1)
      .assetCode("XRP")
      .assetScale(9)
      .description("Test Account")
      .accountRelationship(AccountRelationship.CHILD)
      .linkType(LoopbackLink.LINK_TYPE)
      .sendRoutes(sendRoutes)
      .receiveRoutes(receiveRoutes)
      .build();

  }
}
