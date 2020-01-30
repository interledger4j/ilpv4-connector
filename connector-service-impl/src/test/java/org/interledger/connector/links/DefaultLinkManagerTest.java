package org.interledger.connector.links;

import static org.assertj.core.api.Assertions.assertThat;
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

import com.google.common.eventbus.EventBus;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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

  public ExpectedException expectedException = ExpectedException.none();
  @Mock
  private AccountSettingsRepository accountSettingsRepositoryMock;

  @Mock
  private LinkSettingsFactory linkSettingsFactoryMock;

  @Mock
  private LinkFactoryProvider linkFactoryProviderMock;

  @Mock
  private AccountIdResolver accountIdResolverMock;

  @Mock
  private CircuitBreakerConfig defaultCircuitBreakerConfigMock;

  @Mock
  private LocalDestinationAddressUtils localDestinationAddressUtilsMock;

  @Mock
  private EventBus eventBusMock;

  private DefaultLinkManager defaultLinkManager;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
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
    AccountSettings accountSettingsMock = mock(AccountSettings.class);
    when(accountSettingsMock.accountId()).thenReturn(LocalDestinationAddressUtils.PING_ACCOUNT_ID);
    final Link<?> accountId = defaultLinkManager.getOrCreateLink(accountSettingsMock);

    assertThat(accountId).isEqualTo(LocalDestinationAddressUtils.PING_ACCOUNT_ID);
    Mockito.verifyNoInteractions(linkSettingsFactoryMock);
  }

  @Test
  public void getOrCreateLinkForNonPingLink() {
    AccountSettings accountSettingsMock = mock(AccountSettings.class);
    when(accountSettingsMock.accountId()).thenReturn(AccountId.of("foo"));
    defaultLinkManager.getOrCreateLink(accountSettingsMock);
  }
}
