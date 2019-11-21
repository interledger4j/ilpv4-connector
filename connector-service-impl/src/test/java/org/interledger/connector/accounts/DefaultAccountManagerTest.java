package org.interledger.connector.accounts;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.interledger.connector.links.LinkManager;
import org.interledger.connector.links.LinkSettingsFactory;
import org.interledger.connector.links.LinkSettingsValidator;
import org.interledger.connector.persistence.entities.AccountSettingsEntity;
import org.interledger.connector.persistence.entities.DeletedAccountSettingsEntity;
import org.interledger.connector.persistence.entities.SettlementEngineDetailsEntity;
import org.interledger.connector.persistence.repositories.AccountSettingsRepository;
import org.interledger.connector.persistence.repositories.DeletedAccountSettingsRepository;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.connector.settlement.SettlementEngineClient;
import org.interledger.link.LinkType;

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
      linkSettingsValidator
    );
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

}
