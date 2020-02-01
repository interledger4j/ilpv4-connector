package org.interledger.connector.links;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountIdResolver;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.sub.LocalDestinationAddressUtils;
import org.interledger.connector.persistence.repositories.AccountSettingsRepository;
import org.interledger.core.InterledgerAddress;
import org.interledger.link.Link;
import org.interledger.link.LinkFactoryProvider;
import org.interledger.link.LinkSettings;
import org.interledger.link.PingLoopbackLink;

import com.google.common.eventbus.EventBus;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.function.Supplier;

/**
 * Unit tests for {@link DefaultLinkManager}.
 */
public class DefaultLinkManagerTest {

  // TODO: Once https://github.com/interledger4j/ilpv4-connector/issues/535 is completed, this unit test harness
  //  should be finished. For now, this class does not include extensive unit tests until we decide how to support
  //  Link creation.

  private static final Supplier<InterledgerAddress> INTERLEDGER_ADDRESS_SUPPLIER
    = () -> InterledgerAddress.of("example.connector");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private AccountSettingsRepository accountSettingsRepositoryMock;

  @Mock
  private LinkSettingsFactory linkSettingsFactoryMock;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private LinkFactoryProvider linkFactoryProviderMock;

  @Mock
  private AccountIdResolver accountIdResolverMock;

  @Mock
  private CircuitBreakerConfig defaultCircuitBreakerConfigMock;

  @Mock
  private LocalDestinationAddressUtils localDestinationAddressUtilsMock;

  @Mock
  private EventBus eventBusMock;

  @Mock
  private Link pingLinkMock;

  @Mock
  private Link nonPingLinkMock;

  private DefaultLinkManager defaultLinkManager;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    when(linkFactoryProviderMock.getLinkFactory(any()).constructLink(any(), any())).thenReturn(pingLinkMock);

    this.defaultLinkManager = new DefaultLinkManager(
      INTERLEDGER_ADDRESS_SUPPLIER,
      accountSettingsRepositoryMock,
      linkSettingsFactoryMock,
      linkFactoryProviderMock,
      accountIdResolverMock,
      defaultCircuitBreakerConfigMock,
      localDestinationAddressUtilsMock,
      eventBusMock
    );
  }

  @Test
  public void getOrCreateLinkWithNullAccountSettings() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("accountId must not be null");

    final AccountSettings nullAccountSettings = null;
    defaultLinkManager.getOrCreateLink(nullAccountSettings);
  }

  @Test
  public void getOrCreateLinkForPingLink() {
    when(localDestinationAddressUtilsMock.isConnectorPingAccountId(any())).thenReturn(true);
    when(linkFactoryProviderMock.getLinkFactory(PingLoopbackLink.LINK_TYPE).constructLink(any(), any()))
      .thenReturn(pingLinkMock);

    final AccountSettings accountSettingsMock = mock(AccountSettings.class);
    when(accountSettingsMock.accountId()).thenReturn(LocalDestinationAddressUtils.PING_ACCOUNT_ID);
    final Link<? extends LinkSettings> link = defaultLinkManager.getOrCreateLink(accountSettingsMock);

    assertThat(link).isEqualTo(pingLinkMock);
    Mockito.verifyNoInteractions(linkSettingsFactoryMock);
  }

  @Test
  public void getOrCreateLinkForNonPingLink() {
    final LinkSettings linkSettingsMock = mock(LinkSettings.class);
    when(nonPingLinkMock.getOperatorAddressSupplier()).thenReturn(INTERLEDGER_ADDRESS_SUPPLIER);
    when(nonPingLinkMock.getLinkSettings()).thenReturn(linkSettingsMock);
    when(localDestinationAddressUtilsMock.isConnectorPingAccountId(any())).thenReturn(false);

    when(linkFactoryProviderMock.getLinkFactory(any()).constructLink(any(), any())).thenReturn(nonPingLinkMock);
    when(linkSettingsFactoryMock.construct(any())).thenReturn(linkSettingsMock);

    final AccountSettings accountSettingsMock = mock(AccountSettings.class);
    when(accountSettingsMock.accountId()).thenReturn(AccountId.of("foo"));

    final Link<? extends LinkSettings> link = defaultLinkManager.getOrCreateLink(accountSettingsMock);

    assertThat(link.getLinkId()).isEqualTo(nonPingLinkMock.getLinkId());
  }
}
