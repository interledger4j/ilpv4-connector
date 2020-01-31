package org.interledger.connector.accounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.interledger.connector.links.IldcpFetcherFactory;
import org.interledger.connector.links.LinkManager;
import org.interledger.connector.links.LinkSettingsFactory;
import org.interledger.connector.links.LinkSettingsValidator;
import org.interledger.connector.persistence.entities.AccountSettingsEntity;
import org.interledger.connector.persistence.entities.DeletedAccountSettingsEntity;
import org.interledger.connector.persistence.entities.SettlementEngineDetailsEntity;
import org.interledger.connector.persistence.repositories.AccountSettingsRepository;
import org.interledger.connector.persistence.repositories.DeletedAccountSettingsRepository;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.connector.settings.ModifiableConnectorSettings;
import org.interledger.connector.settings.properties.ConnectorSettingsFromPropertyFile;
import org.interledger.connector.settlement.SettlementEngineClient;
import org.interledger.core.InterledgerAddress;
import org.interledger.ildcp.IldcpFetcher;
import org.interledger.ildcp.IldcpResponse;
import org.interledger.link.Link;
import org.interledger.link.LinkType;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IlpOverHttpLinkSettings;
import org.interledger.link.http.IncomingLinkSettings;
import org.interledger.link.http.OutgoingLinkSettings;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.core.convert.ConversionService;

import java.util.Optional;

public class DefaultAccountManagerTest {

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private ConnectorSettings connectorSettings;
  @Mock
  private AccountSettingsRepository accountSettingsRepository;
  @Mock
  private DeletedAccountSettingsRepository deletedAccountSettingsRepository;
  @Mock
  private LinkManager linkManager;
  @Mock
  private ConversionService conversionService;
  @Mock
  private SettlementEngineClient settlementEngineClient;
  @Mock
  private LinkSettingsValidator linkSettingsValidator;
  @Mock
  private LinkSettingsFactory linkSettingsFactory;
  @Mock
  private IldcpFetcherFactory ildcpFetcherFactory;

  private DefaultAccountManager accountManager;

  @Before
  public void setUp() {
    accountManager = new DefaultAccountManager(
      () -> connectorSettings,
      conversionService,
      accountSettingsRepository,
      deletedAccountSettingsRepository,
      linkManager,
      settlementEngineClient,
      linkSettingsFactory,
      linkSettingsValidator,
      ildcpFetcherFactory);
  }

  @Test
  public void failOnDupeSettlementEngineId() {
    AccountSettingsEntity entity = mock(AccountSettingsEntity.class);
    SettlementEngineDetailsEntity engineEntity = mock(SettlementEngineDetailsEntity.class);
    when(entity.getSettlementEngineDetailsEntity()).thenReturn(engineEntity);
    String daveandbusters = "daveandbusters";
    when(engineEntity.getSettlementEngineAccountId()).thenReturn(daveandbusters);
    AccountId mac = AccountId.of("mac");
    when(entity.getAccountId()).thenReturn(mac);

    when(accountSettingsRepository.save(entity))
      .thenThrow(new AccountSettlementEngineAlreadyExistsProblem(mac, daveandbusters));
    expectedException.expect(AccountSettlementEngineAlreadyExistsProblem.class);
    expectedException.expectMessage("Account Settlement Engine Already Exists " +
      "[accountId: `" + mac.value() + "`, settlementEngineId: `" + daveandbusters + "`]");
    accountManager.persistAccountSettingsEntity(entity);
  }

  @Test
  public void validateLinkSettingsFailsOnInvalidLinkType() {
    AccountSettings accountSettings = AccountSettings.builder()
      .accountId(AccountId.of("UnsupportedLinkTypeGuy"))
      .assetCode("XRP")
      .assetScale(9)
      .linkType(LinkType.of("UnsupportedLinkType"))
      .accountRelationship(AccountRelationship.PEER)
      .build();

    expectedException.expect(InvalidAccountSettingsProblem.class);
    expectedException.expectMessage("Unsupported LinkType:");

    when(linkSettingsFactory.constructTyped(accountSettings)).thenThrow(new IllegalArgumentException("Unsupported LinkType: " + accountSettings.linkType()));

    accountManager.validateLinkSettings(accountSettings);
  }

  @Test
  public void deleteAccount() {
    AccountId accountId = AccountId.of("egg");
    final AccountSettings accountSettings = AccountSettings.builder()
      .accountId(accountId)
      .assetCode("XRP")
      .assetScale(9)
      .linkType(LinkType.of("Loopback"))
      .accountRelationship(AccountRelationship.PEER)
      .build();
    AccountSettingsEntity account = new AccountSettingsEntity(accountSettings);

    when(accountSettingsRepository.findByAccountId(accountId)).thenReturn(Optional.of(account));
    accountManager.deleteByAccountId(accountId);
    verify(accountSettingsRepository, times(1)).findByAccountId(accountId);
    verify(deletedAccountSettingsRepository, times(1))
      .save(new DeletedAccountSettingsEntity(account));
  }

  @Test
  public void deleteMissingAccountThrowsNotFound() {
    AccountId accountId = AccountId.of("egg");
    when(accountSettingsRepository.findByAccountId(accountId)).thenReturn(Optional.empty());
    expectedException.expect(AccountNotFoundProblem.class);
    accountManager.deleteByAccountId(accountId);
  }

  @Test
  public void updateAccount() {
    AccountId accountId = AccountId.of("lloyd");
    LinkType linkType = IlpOverHttpLink.LINK_TYPE;

    IlpOverHttpLinkSettings unvalidatedSettings = IlpOverHttpLinkSettings.builder()
      .incomingLinkSettings(mock(IncomingLinkSettings.class))
      .outgoingLinkSettings(mock(OutgoingLinkSettings.class))
      .customSettings(ImmutableMap.of("foo", "bar"))
      .build();

    IlpOverHttpLinkSettings validatedSettings = IlpOverHttpLinkSettings.builder()
      .incomingLinkSettings(mock(IncomingLinkSettings.class))
      .outgoingLinkSettings(mock(OutgoingLinkSettings.class))
      .customSettings(ImmutableMap.of("fizz", "buzz"))
      .build();

    final AccountSettings accountSettings = AccountSettings.builder()
      .accountId(accountId)
      .assetCode("XRP")
      .assetScale(9)
      .linkType(linkType)
      .accountRelationship(AccountRelationship.PEER)
      .customSettings(unvalidatedSettings.getCustomSettings())
      .build();
    AccountSettingsEntity account = new AccountSettingsEntity(accountSettings);

    final AccountSettings expectedUpdatedSettings = AccountSettings.builder()
      .from(accountSettings)
      .customSettings(validatedSettings.getCustomSettings())
      .build();
    AccountSettingsEntity expectedEntityToUpdate = new AccountSettingsEntity(expectedUpdatedSettings);

    when(accountSettingsRepository.findByAccountId(accountId)).thenReturn(Optional.of(account));
    when(accountSettingsRepository.save(
      argThat((arg) -> arg.getCustomSettings().equals(validatedSettings.getCustomSettings()))))
      .thenReturn(expectedEntityToUpdate);
    when(linkSettingsValidator.validateSettings(unvalidatedSettings)).thenReturn(validatedSettings);
    when(linkSettingsFactory.constructTyped(accountSettings)).thenReturn(unvalidatedSettings);
    when(conversionService.convert(expectedEntityToUpdate, AccountSettings.class)).thenReturn(expectedUpdatedSettings);
    AccountSettings updated = accountManager.updateAccount(accountId, accountSettings);

    assertThat(updated).isEqualTo(expectedUpdatedSettings);
    verify(accountSettingsRepository, times(1)).findByAccountId(accountId);
    verify(accountSettingsRepository, times(1)).save(expectedEntityToUpdate);
  }

  @Test
  public void initializeParentAccountSettingsViaIlDcpWithModifiableConfig() {
    ModifiableConnectorSettings modifiableConnectorSettings = ModifiableConnectorSettings.create();
    InterledgerAddress ildcpAddress = InterledgerAddress.of("g.parent.child");
    AccountId accountId = simulateIldcp(modifiableConnectorSettings, ildcpAddress);

    assertThat(modifiableConnectorSettings.operatorAddress()).isEqualTo(InterledgerAddress.of("self.node"));
    accountManager.initializeParentAccountSettingsViaIlDcp(accountId);
    assertThat(modifiableConnectorSettings.operatorAddress()).isEqualTo(ildcpAddress);
  }

  @Test
  public void initializeParentAccountSettingsViaIlDcpWithPropertiesConfig() {
    ConnectorSettingsFromPropertyFile connectorSettingsFromPropertyFile = new ConnectorSettingsFromPropertyFile();
    InterledgerAddress ildcpAddress = InterledgerAddress.of("g.parent.child");
    AccountId accountId = simulateIldcp(connectorSettingsFromPropertyFile, ildcpAddress);

    assertThat(connectorSettingsFromPropertyFile.operatorAddress()).isEqualTo(InterledgerAddress.of("self.node"));
    accountManager.initializeParentAccountSettingsViaIlDcp(accountId);
    assertThat(connectorSettingsFromPropertyFile.operatorAddress()).isEqualTo(ildcpAddress);
  }

  private AccountId simulateIldcp(ConnectorSettings modifiableConnectorSettings, InterledgerAddress ildcpAddress) {
    accountManager = new DefaultAccountManager(
      () -> modifiableConnectorSettings,
      conversionService,
      accountSettingsRepository,
      deletedAccountSettingsRepository,
      linkManager,
      settlementEngineClient,
      linkSettingsFactory,
      linkSettingsValidator,
      ildcpFetcherFactory);

    AccountId accountId = AccountId.of("child");
    LinkType linkType = IlpOverHttpLink.LINK_TYPE;
    Link mockLink = mock(IlpOverHttpLink.class);
    IldcpFetcher mockIldcpFetcher = mock(IldcpFetcher.class);

    IlpOverHttpLinkSettings validatedSettings = IlpOverHttpLinkSettings.builder()
      .incomingLinkSettings(mock(IncomingLinkSettings.class))
      .outgoingLinkSettings(mock(OutgoingLinkSettings.class))
      .customSettings(ImmutableMap.of("fizz", "buzz"))
      .build();

    final AccountSettings accountSettings = AccountSettings.builder()
      .accountId(accountId)
      .assetCode("XRP")
      .assetScale(9)
      .linkType(linkType)
      .accountRelationship(AccountRelationship.PEER)
      .customSettings(validatedSettings.getCustomSettings())
      .build();
    AccountSettingsEntity account = new AccountSettingsEntity(accountSettings);

    when(accountSettingsRepository.safeFindByAccountId(accountId)).thenReturn(account);
    when(conversionService.convert(account, AccountSettings.class)).thenReturn(accountSettings);
    when(linkManager.getOrCreateLink(accountSettings)).thenReturn(mockLink);
    when(accountSettingsRepository.findByAccountId(accountId)).thenReturn(Optional.of(account));
    when(ildcpFetcherFactory.construct(mockLink)).thenReturn(mockIldcpFetcher);

    when(mockIldcpFetcher.fetch(any())).thenReturn(IldcpResponse.builder()
      .assetCode("XRP")
      .assetScale((short) 9)
      .clientAddress(ildcpAddress)
      .build()
    );
    return accountId;
  }

}
