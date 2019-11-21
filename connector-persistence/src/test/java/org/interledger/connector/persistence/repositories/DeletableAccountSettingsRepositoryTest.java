package org.interledger.connector.persistence.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.persistence.config.ConnectorPersistenceConfig;
import org.interledger.connector.persistence.converters.AccountBalanceSettingsEntityConverter;
import org.interledger.connector.persistence.converters.AccountSettingsEntityConverter;
import org.interledger.connector.persistence.converters.RateLimitSettingsEntityConverter;
import org.interledger.connector.persistence.converters.SettlementEngineDetailsEntityConverter;
import org.interledger.connector.persistence.entities.AccountSettingsEntity;
import org.interledger.connector.persistence.entities.DeletedAccountSettingsEntity;
import org.interledger.link.LinkType;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Unit tests for {@link AccountSettingsRepository}.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
  ConnectorPersistenceConfig.class, DeletableAccountSettingsRepositoryTest.TestPersistenceConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
@AutoConfigureEmbeddedDatabase
public class DeletableAccountSettingsRepositoryTest {

  @Autowired
  private DeletedAccountSettingsRepository deletedAccountSettingsRepository;

  @Test
  public void saveAndFind() {
    AccountId accountId = AccountId.of("egg");
    final AccountSettings accountSettings = AccountSettings.builder()
      .accountId(accountId)
      .assetCode("XRP")
      .assetScale(9)
      .linkType(LinkType.of("Loopback"))
      .accountRelationship(AccountRelationship.PEER)
      .build();
    AccountSettingsEntity accountEntity = new AccountSettingsEntity(accountSettings);
    DeletedAccountSettingsEntity deletedAccountEntity = new DeletedAccountSettingsEntity(accountEntity);
    DeletedAccountSettingsEntity saved = deletedAccountSettingsRepository.save(deletedAccountEntity);
    DeletedAccountSettingsEntity found = deletedAccountSettingsRepository.findById(saved.getId()).get();
    assertThat(saved).extracting("accountId", "assetCode", "assetScale")
      .containsExactly(found.getAccountId(), found.getAssetCode(), found.getAssetScale());

  }

  @Configuration("application.yml")
  public static class TestPersistenceConfig {
    ////////////////////////
    // SpringConverters
    ////////////////////////

    @Autowired
    private RateLimitSettingsEntityConverter rateLimitSettingsEntityConverter;

    @Autowired
    private AccountBalanceSettingsEntityConverter accountBalanceSettingsEntityConverter;

    @Autowired
    private SettlementEngineDetailsEntityConverter settlementEngineDetailsEntityConverter;

    @Autowired
    private AccountSettingsEntityConverter accountSettingsConverter;

    @Bean
    protected ObjectMapper objectMapper() {
      return new ObjectMapper();
    }

    @Bean
    protected ConfigurableConversionService conversionService() {
      ConfigurableConversionService conversionService = new DefaultConversionService();
      conversionService.addConverter(rateLimitSettingsEntityConverter);
      conversionService.addConverter(accountBalanceSettingsEntityConverter);
      conversionService.addConverter(settlementEngineDetailsEntityConverter);
      conversionService.addConverter(accountSettingsConverter);
      return conversionService;
    }

  }
}
