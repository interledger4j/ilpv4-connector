package org.interledger.connector.persistence.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.connector.persistence.entities.DataConstants.ColumnNames.ACCOUNT_ID;
import static org.interledger.connector.persistence.entities.DataConstants.TableNames.ACCOUNT_SETTINGS;

import org.interledger.connector.accounts.AccountBalanceSettings;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRateLimitSettings;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.SettlementEngineAccountId;
import org.interledger.connector.accounts.SettlementEngineDetails;
import org.interledger.connector.persistence.config.ConnectorPersistenceConfig;
import org.interledger.connector.persistence.converters.AccountBalanceSettingsEntityConverter;
import org.interledger.connector.persistence.converters.AccountSettingsEntityConverter;
import org.interledger.connector.persistence.converters.RateLimitSettingsEntityConverter;
import org.interledger.connector.persistence.converters.SettlementEngineDetailsEntityConverter;
import org.interledger.connector.persistence.entities.AccountSettingsEntity;
import org.interledger.connector.persistence.entities.SettlementEngineDetailsEntity;
import org.interledger.link.LinkType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.common.primitives.UnsignedLong;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import okhttp3.HttpUrl;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.Query;

/**
 * Unit tests for {@link AccountSettingsRepository}.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
  ConnectorPersistenceConfig.class, AccountSettingsRepositoryTest.TestPersistenceConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
@AutoConfigureEmbeddedDatabase
public class AccountSettingsRepositoryTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Autowired
  private AccountSettingsRepository accountSettingsRepository;

  @Autowired
  private EntityManager entityManager;

  @Test
  public void whenSaveAndLoadWithAllFieldsPopulated() {
    final Map<String, Object> customSettings = Maps.newHashMap();
    customSettings.put("address", "123 Main Street");
    customSettings.put("zipcode", 12345);

    final AccountSettings accountSettings = AccountSettings.builder()
      .accountId(AccountId.of(generateUuid()))
      .description("description")
      .assetCode("XRP")
      .assetScale(9)
      .maximumPacketAmount(UnsignedLong.valueOf(10L))
      .linkType(LinkType.of("Loopback"))
      .isConnectionInitiator(true)
      .isInternal(true)
      .accountRelationship(AccountRelationship.PEER)
      .rateLimitSettings(AccountRateLimitSettings.builder()
        .maxPacketsPerSecond(10)
        .build())
      .balanceSettings(AccountBalanceSettings.builder()
        .minBalance(0L)
        .settleThreshold(100L)
        .settleTo(10L)
        .build())
      .settlementEngineDetails(SettlementEngineDetails.builder()
        .baseUrl(HttpUrl.parse("https://example.com"))
        .settlementEngineAccountId(SettlementEngineAccountId.of(generateUuid()))
        .putCustomSettings("foo", "bar")
        .build())
      .ilpAddressSegment("foo")
      .customSettings(customSettings)
      .build();

    final AccountSettingsEntity accountSettingsEntity = new AccountSettingsEntity(accountSettings);
    assertThat(accountSettingsEntity.getId()).isEqualTo(null);
    assertThat(accountSettingsEntity.getAccountId()).isEqualTo(accountSettings.accountId());
    assertAllFieldsEqual(accountSettingsEntity, new AccountSettingsEntity(accountSettings));

    // Equals methods are not the same, so verify this.
    assertThat(accountSettingsEntity).isNotEqualTo(accountSettings);

    final AccountSettingsEntity savedAccountSettingsEntity = accountSettingsRepository.save(accountSettingsEntity);
    assertThat(savedAccountSettingsEntity).isEqualTo(accountSettingsEntity);
    assertThat(savedAccountSettingsEntity.getId()).isGreaterThan(0);
    assertThat(savedAccountSettingsEntity.getAccountId()).isEqualTo(accountSettings.accountId());
    assertAllFieldsEqual(savedAccountSettingsEntity, new AccountSettingsEntity(accountSettings));

    final AccountSettingsEntity loadedAccountSettingsEntity =
      accountSettingsRepository.findById(savedAccountSettingsEntity.getId()).get();
    assertThat(loadedAccountSettingsEntity.getId()).isGreaterThan(0);
    assertThat(loadedAccountSettingsEntity.getAccountId()).isEqualTo(accountSettings.accountId());
    assertAllFieldsEqual(loadedAccountSettingsEntity, new AccountSettingsEntity(accountSettings));

    final AccountSettingsEntity loadedAccountSettingsEntity2 =
      accountSettingsRepository.findByAccountId(accountSettings.accountId().value()).get();
    assertThat(loadedAccountSettingsEntity2.getId()).isEqualTo(loadedAccountSettingsEntity.getId());
    assertThat(loadedAccountSettingsEntity2.getAccountId()).isEqualTo(loadedAccountSettingsEntity.getAccountId());
    assertAllFieldsEqual(loadedAccountSettingsEntity2, new AccountSettingsEntity(accountSettings));
  }

  @Test
  public void whenSaveAndLoadWithMinimalFieldsPopulated() {
    final AccountId accountId = AccountId.of(generateUuid());
    final AccountSettings accountSettings = AccountSettings.builder()
      .accountId(accountId)
      .assetCode("XRP")
      .assetScale(9)
      .linkType(LinkType.of("Loopback"))
      .accountRelationship(AccountRelationship.PEER)
      .build();

    final AccountSettingsEntity accountSettingsEntity = new AccountSettingsEntity(accountSettings);
    assertThat(accountSettingsEntity.getId()).isNull();
    assertThat(accountSettingsEntity.getAccountId()).isEqualTo(accountSettings.accountId());
    assertAllFieldsEqual(accountSettingsEntity, new AccountSettingsEntity(accountSettings));

    // Equals methods are not the same, so verify this.
    assertThat(accountSettingsEntity).isNotEqualTo(accountSettings);

    final AccountSettingsEntity savedAccountSettingsEntity = accountSettingsRepository.save(accountSettingsEntity);
    assertThat(savedAccountSettingsEntity).isEqualTo(accountSettingsEntity);
    assertThat(savedAccountSettingsEntity.getId()).isGreaterThan(0);
    assertThat(savedAccountSettingsEntity.getAccountId()).isEqualTo(accountSettings.accountId());
    assertAllFieldsEqual(savedAccountSettingsEntity, new AccountSettingsEntity(accountSettings));

    final AccountSettingsEntity loadedAccountSettingsEntity =
      accountSettingsRepository.findById(savedAccountSettingsEntity.getId()).get();
    assertThat(loadedAccountSettingsEntity.getId()).isGreaterThan(0);
    assertThat(loadedAccountSettingsEntity.getAccountId()).isEqualTo(accountSettings.accountId());
    assertAllFieldsEqual(loadedAccountSettingsEntity, new AccountSettingsEntity(accountSettings));

    // Assert actual loaded values...
    assertThat(loadedAccountSettingsEntity.getAccountRelationship()).isEqualTo(AccountRelationship.PEER);
    assertThat(loadedAccountSettingsEntity.getLinkType()).isEqualTo(LinkType.of("Loopback"));
    assertThat(loadedAccountSettingsEntity.getAssetCode()).isEqualTo("XRP");
    assertThat(loadedAccountSettingsEntity.getAssetScale()).isEqualTo(9);
    assertThat(loadedAccountSettingsEntity.getCustomSettings().size()).isEqualTo(0);
    assertThat(loadedAccountSettingsEntity.getMaximumPacketAmount().isPresent()).isFalse();
    assertThat(loadedAccountSettingsEntity.getBalanceSettings().getMinBalance().isPresent()).isFalse();
    assertThat(loadedAccountSettingsEntity.getBalanceSettings().getSettleThreshold().isPresent()).isFalse();
    assertThat(loadedAccountSettingsEntity.getBalanceSettings().getSettleTo()).isZero();
    assertThat(loadedAccountSettingsEntity.getRateLimitSettings().getMaxPacketsPerSecond().isPresent()).isFalse();
    assertThat(loadedAccountSettingsEntity.settlementEngineDetails().isPresent()).isFalse();
    assertThat(loadedAccountSettingsEntity.getIlpAddressSegment()).isEqualTo(accountId.value());

    final AccountSettingsEntity loadedAccountSettingsEntity2 =
      accountSettingsRepository.findByAccountId(accountSettings.accountId().value()).get();
    assertThat(loadedAccountSettingsEntity2.getId()).isEqualTo(loadedAccountSettingsEntity.getId());
    assertThat(loadedAccountSettingsEntity2.getAccountId()).isEqualTo(loadedAccountSettingsEntity.getAccountId());
    assertAllFieldsEqual(loadedAccountSettingsEntity2, new AccountSettingsEntity(accountSettings));

    assertThat(loadedAccountSettingsEntity.getAccountRelationship()).isEqualTo(AccountRelationship.PEER);
    assertThat(loadedAccountSettingsEntity.getLinkType()).isEqualTo(LinkType.of("Loopback"));
    assertThat(loadedAccountSettingsEntity.getAssetCode()).isEqualTo("XRP");
    assertThat(loadedAccountSettingsEntity.getAssetScale()).isEqualTo(9);
    assertThat(loadedAccountSettingsEntity.getCustomSettings().size()).isZero();
    assertThat(loadedAccountSettingsEntity.getMaximumPacketAmount().isPresent()).isFalse();
    assertThat(loadedAccountSettingsEntity.getBalanceSettings().getMinBalance().isPresent()).isFalse();
    assertThat(loadedAccountSettingsEntity.getBalanceSettings().getSettleThreshold().isPresent()).isFalse();
    assertThat(loadedAccountSettingsEntity.getBalanceSettings().getSettleTo()).isZero();
    assertThat(loadedAccountSettingsEntity.getRateLimitSettings().getMaxPacketsPerSecond().isPresent()).isFalse();
    assertThat(loadedAccountSettingsEntity.getIlpAddressSegment()).isEqualTo(accountId.value());
  }

  @Test
  public void findBySettlementEngineAccountId() {
    final SettlementEngineAccountId settlementEngineAccountId =
      SettlementEngineAccountId.of(generateUuid());

    final AccountSettings accountSettings1 = AccountSettings.builder()
      .accountId(AccountId.of(generateUuid()))
      .assetCode("XRP")
      .assetScale(9)
      .linkType(LinkType.of("Loopback"))
      .accountRelationship(AccountRelationship.PEER)
      .settlementEngineDetails(
        SettlementEngineDetails.builder()
          .baseUrl(HttpUrl.parse("https://example.com"))
          .settlementEngineAccountId(settlementEngineAccountId)
          .build()
      )
      .build();
    final AccountSettingsEntity accountSettingsEntity = new AccountSettingsEntity(accountSettings1);
    accountSettingsRepository.save(accountSettingsEntity);

    Optional<AccountSettingsEntity> actual = accountSettingsRepository
      .findBySettlementEngineAccountId(settlementEngineAccountId);
    assertThat(actual.isPresent()).isTrue();

    this.assertAllFieldsEqual(actual.get(), accountSettingsEntity);
  }

  @Test
  public void findBySettlementEngineAccountIdWhenNonExistent() {
    final SettlementEngineAccountId settlementEngineAccountId =
      SettlementEngineAccountId.of(generateUuid());

    assertThat(
      accountSettingsRepository.findBySettlementEngineAccountId(settlementEngineAccountId)
        .isPresent()
    ).isFalse();
  }

  @Test
  public void findBySettlementEngineAccountIdWhenIdIsNull() {
    final SettlementEngineAccountId settlementEngineAccountId =
      SettlementEngineAccountId.of(generateUuid());

    final AccountSettings accountSettings = AccountSettings.builder()
      .accountId(AccountId.of(generateUuid()))
      .assetCode("XRP")
      .assetScale(9)
      .linkType(LinkType.of("Loopback"))
      .accountRelationship(AccountRelationship.PEER)
      .build();
    final AccountSettingsEntity accountSettingsEntity = new AccountSettingsEntity(accountSettings);

    // Construct a SettlementEngineDetailsEntity with null values....
    SettlementEngineDetailsEntity nullValueSettlementEngineDetailsEntity = new SettlementEngineDetailsEntity(
      SettlementEngineDetails.builder()
        .baseUrl(HttpUrl.parse("https://example.com"))
        .settlementEngineAccountId(settlementEngineAccountId)
        .build()
    );
    nullValueSettlementEngineDetailsEntity.setBaseUrl(null);
    nullValueSettlementEngineDetailsEntity.setSettlementEngineAccountId(null);
    accountSettingsEntity.setSettlementEngineDetails(nullValueSettlementEngineDetailsEntity);
    accountSettingsRepository.save(accountSettingsEntity);

    assertThat(
      accountSettingsRepository.findBySettlementEngineAccountId(settlementEngineAccountId)
        .isPresent()
    ).isFalse();
  }

  @Test
  public void whenFindFirstByAccountRelationship() {
    final AccountSettings accountSettings1 = AccountSettings.builder()
      .accountId(AccountId.of(generateUuid()))
      .assetCode("XRP")
      .assetScale(9)
      .linkType(LinkType.of("Loopback"))
      .accountRelationship(AccountRelationship.PEER)
      .build();
    final AccountSettingsEntity accountSettingsEntity1 = new AccountSettingsEntity(accountSettings1);
    accountSettingsRepository.save(accountSettingsEntity1);
    assertThat(accountSettingsRepository.findFirstByAccountRelationship(AccountRelationship.PARENT).isPresent())
      .isFalse();
    assertThat(accountSettingsRepository.findFirstByAccountRelationship(AccountRelationship.CHILD).isPresent())
      .isFalse();
    assertThat(accountSettingsRepository.findFirstByAccountRelationship(AccountRelationship.PEER).isPresent())
      .isTrue();

    final AccountSettings accountSettings1b = AccountSettings.builder()
      .accountId(AccountId.of(generateUuid()))
      .assetCode("XRP")
      .assetScale(9)
      .linkType(LinkType.of("Loopback"))
      .accountRelationship(AccountRelationship.CHILD)
      .build();
    final AccountSettingsEntity accountSettingsEntity1b = new AccountSettingsEntity(accountSettings1b);
    accountSettingsRepository.save(accountSettingsEntity1b);
    assertThat(accountSettingsRepository.findFirstByAccountRelationship(AccountRelationship.PARENT).isPresent())
      .isFalse();
    assertThat(accountSettingsRepository.findFirstByAccountRelationship(AccountRelationship.CHILD).isPresent())
      .isTrue();
    assertThat(accountSettingsRepository.findFirstByAccountRelationship(AccountRelationship.PEER).isPresent()).isTrue();

    final AccountSettings accountSettings2 = AccountSettings.builder()
      .accountId(AccountId.of(generateUuid()))
      .assetCode("XRP")
      .assetScale(9)
      .linkType(LinkType.of("Loopback"))
      .accountRelationship(AccountRelationship.PARENT)
      .build();
    final AccountSettingsEntity accountSettingsEntity2 = new AccountSettingsEntity(accountSettings2);
    accountSettingsRepository.save(accountSettingsEntity2);
    assertThat(accountSettingsRepository.findFirstByAccountRelationship(AccountRelationship.PARENT).get())
      .isEqualTo(accountSettingsEntity2); // Always finds the first `PARENT`
    assertThat(accountSettingsRepository.findFirstByAccountRelationship(AccountRelationship.CHILD).isPresent())
      .isTrue();
    assertThat(accountSettingsRepository.findFirstByAccountRelationship(AccountRelationship.PEER).isPresent()).isTrue();

    final AccountSettings accountSettings3 = AccountSettings.builder()
      .accountId(AccountId.of(generateUuid()))
      .assetCode("XRP")
      .assetScale(9)
      .linkType(LinkType.of("Loopback"))
      .accountRelationship(AccountRelationship.PARENT)
      .build();
    final AccountSettingsEntity accountSettingsEntity3 = new AccountSettingsEntity(accountSettings3);
    accountSettingsRepository.save(accountSettingsEntity3);
    assertThat(accountSettingsRepository.findFirstByAccountRelationship(AccountRelationship.PARENT).get())
      .isEqualTo(accountSettingsEntity2); // Always finds the first `PARENT`
    assertThat(accountSettingsRepository.findFirstByAccountRelationship(AccountRelationship.CHILD).isPresent())
      .isTrue();
    assertThat(accountSettingsRepository.findFirstByAccountRelationship(AccountRelationship.PEER).isPresent()).isTrue();

    final AccountSettings accountSettings4 = AccountSettings.builder()
      .accountId(AccountId.of(generateUuid()))
      .assetCode("XRP")
      .assetScale(9)
      .linkType(LinkType.of("Loopback"))
      .accountRelationship(AccountRelationship.PARENT)
      .build();
    final AccountSettingsEntity accountSettingsEntity4 = new AccountSettingsEntity(accountSettings4);
    accountSettingsRepository.save(accountSettingsEntity4);
    assertThat(accountSettingsRepository.findFirstByAccountRelationship(AccountRelationship.PARENT).get())
      .isEqualTo(accountSettingsEntity2); // Always finds the first `PARENT`
    assertThat(accountSettingsRepository.findFirstByAccountRelationship(AccountRelationship.CHILD).isPresent())
      .isTrue();
    assertThat(accountSettingsRepository.findFirstByAccountRelationship(AccountRelationship.PEER).isPresent()).isTrue();
  }

  @Test
  public void whenFindAllByAccountRelationship() {
    final AccountSettings accountSettings1 = AccountSettings.builder()
      .accountId(AccountId.of(generateUuid()))
      .assetCode("XRP")
      .assetScale(9)
      .linkType(LinkType.of("Loopback"))
      .accountRelationship(AccountRelationship.PEER)
      .build();
    final AccountSettingsEntity accountSettingsEntity1 = new AccountSettingsEntity(accountSettings1);
    accountSettingsRepository.save(accountSettingsEntity1);
    assertThat(accountSettingsRepository.findByAccountRelationshipIs(AccountRelationship.PARENT).size()).isZero();
    assertThat(accountSettingsRepository.findByAccountRelationshipIs(AccountRelationship.CHILD).size()).isZero();
    assertThat(accountSettingsRepository.findByAccountRelationshipIs(AccountRelationship.PEER).size()).isOne();

    final AccountSettings accountSettings2 = AccountSettings.builder()
      .accountId(AccountId.of(generateUuid()))
      .assetCode("XRP")
      .assetScale(9)
      .linkType(LinkType.of("Loopback"))
      .accountRelationship(AccountRelationship.PARENT)
      .build();
    final AccountSettingsEntity accountSettingsEntity2 = new AccountSettingsEntity(accountSettings2);
    accountSettingsRepository.save(accountSettingsEntity2);
    assertThat(accountSettingsRepository.findByAccountRelationshipIs(AccountRelationship.PARENT).size()).isOne();
    assertThat(accountSettingsRepository.findByAccountRelationshipIs(AccountRelationship.CHILD).size()).isZero();
    assertThat(accountSettingsRepository.findByAccountRelationshipIs(AccountRelationship.PEER).size()).isOne();

    final AccountSettings accountSettings3 = AccountSettings.builder()
      .accountId(AccountId.of(generateUuid()))
      .assetCode("XRP")
      .assetScale(9)
      .linkType(LinkType.of("Loopback"))
      .accountRelationship(AccountRelationship.CHILD)
      .build();
    final AccountSettingsEntity accountSettingsEntity3 = new AccountSettingsEntity(accountSettings3);
    accountSettingsRepository.save(accountSettingsEntity3);
    assertThat(accountSettingsRepository.findByAccountRelationshipIs(AccountRelationship.PARENT).size()).isOne();
    assertThat(accountSettingsRepository.findByAccountRelationshipIs(AccountRelationship.CHILD).size()).isOne();
    assertThat(accountSettingsRepository.findByAccountRelationshipIs(AccountRelationship.PEER).size()).isOne();
  }

  // Execute these three tests individually to isolate out any alternatives that might provide false-positives.

  @Test
  public void whenFindAllByAccountRelationshipIsPeer() {
    final AccountSettings accountSettings1 = AccountSettings.builder()
      .accountId(AccountId.of(generateUuid()))
      .assetCode("XRP")
      .assetScale(9)
      .linkType(LinkType.of("Loopback"))
      .accountRelationship(AccountRelationship.PEER)
      .build();
    final AccountSettingsEntity accountSettingsEntity1 = new AccountSettingsEntity(accountSettings1);
    accountSettingsRepository.save(accountSettingsEntity1);
    assertThat(accountSettingsRepository.findByAccountRelationshipIs(AccountRelationship.PARENT).size()).isZero();
    assertThat(accountSettingsRepository.findByAccountRelationshipIs(AccountRelationship.CHILD).size()).isZero();
    assertThat(accountSettingsRepository.findByAccountRelationshipIs(AccountRelationship.PEER).size()).isOne();
  }

  @Test
  public void whenFindAllByAccountRelationshipParent() {

    final AccountSettings accountSettings2 = AccountSettings.builder()
      .accountId(AccountId.of(generateUuid()))
      .assetCode("XRP")
      .assetScale(9)
      .linkType(LinkType.of("Loopback"))
      .accountRelationship(AccountRelationship.PARENT)
      .build();
    final AccountSettingsEntity accountSettingsEntity2 = new AccountSettingsEntity(accountSettings2);
    accountSettingsRepository.save(accountSettingsEntity2);
    assertThat(accountSettingsRepository.findByAccountRelationshipIs(AccountRelationship.PARENT).size()).isOne();
    assertThat(accountSettingsRepository.findByAccountRelationshipIs(AccountRelationship.CHILD).size()).isZero();
    assertThat(accountSettingsRepository.findByAccountRelationshipIs(AccountRelationship.PEER).size()).isZero();
  }

  @Test
  public void whenFindAllByAccountRelationshipChild() {
    final AccountSettings accountSettings3 = AccountSettings.builder()
      .accountId(AccountId.of(generateUuid()))
      .assetCode("XRP")
      .assetScale(9)
      .linkType(LinkType.of("Loopback"))
      .accountRelationship(AccountRelationship.CHILD)
      .build();
    final AccountSettingsEntity accountSettingsEntity3 = new AccountSettingsEntity(accountSettings3);
    accountSettingsRepository.save(accountSettingsEntity3);
    assertThat(accountSettingsRepository.findByAccountRelationshipIs(AccountRelationship.PARENT).size()).isZero();
    assertThat(accountSettingsRepository.findByAccountRelationshipIs(AccountRelationship.CHILD).size()).isOne();
    assertThat(accountSettingsRepository.findByAccountRelationshipIs(AccountRelationship.PEER).size()).isZero();
  }

  @Test
  public void whenAccountSettingsAlreadyExists() {
    final AccountId accountId = AccountId.of(generateUuid());
    final AccountSettings accountSettings1 = AccountSettings.builder()
      .accountId(accountId)
      .assetCode("XRP")
      .assetScale(9)
      .linkType(LinkType.of("Loopback"))
      .accountRelationship(AccountRelationship.PEER)
      .build();
    final AccountSettingsEntity accountSettingsEntity1 = new AccountSettingsEntity(accountSettings1);
    accountSettingsRepository.save(accountSettingsEntity1);

    AccountSettingsEntity loadedEntity = accountSettingsRepository.findByAccountId(accountId).get();
    assertThat(loadedEntity.getAccountId()).isEqualTo(accountSettings1.accountId());

    final AccountSettingsEntity duplicateEntity = new AccountSettingsEntity(accountSettings1);
    assertThat(duplicateEntity.getAccountId()).isEqualTo(accountSettings1.accountId());

    expectedException.expect(DataIntegrityViolationException.class);
    expectedException.expectMessage("could not execute statement");
    accountSettingsRepository.save(duplicateEntity);
    accountSettingsRepository.findAll(); // Triggers the flush
  }

  @Test
  public void findByIdWithConversion() {
    final AccountId accountId = AccountId.of(generateUuid());
    final AccountSettings accountSettings1 = AccountSettings.builder()
      .accountId(accountId)
      .assetCode("XRP")
      .assetScale(9)
      .linkType(LinkType.of("Loopback"))
      .accountRelationship(AccountRelationship.PEER)
      .build();
    final AccountSettingsEntity accountSettingsEntity1 = new AccountSettingsEntity(accountSettings1);
    accountSettingsRepository.save(accountSettingsEntity1);

    Optional<AccountSettings> loadedAccountSettings =
      accountSettingsRepository.findByAccountIdWithConversion(accountId);
    assertThat(loadedAccountSettings.isPresent()).isTrue();

    assertThat(loadedAccountSettings.get()).isEqualTo(accountSettings1);
  }

  /**
   * This test validates that account_settings database records that have an invalid identifier are ignored so that
   * accoutIds that cannot be marshalled to an {@link AccountId} do not blow up the entire query system.
   */
  @Test
  public void findFirstByAccountRelationshipWithConversionWhenIdIsInvalid() {
    final AccountId accountId1 = AccountId.of(generateUuid());
    final String invalidAccountId = "foo+bar";
    {
      final AccountSettings accountSettings1 = AccountSettings.builder()
        .accountId(accountId1)
        .assetCode("XRP")
        .assetScale(9)
        .linkType(LinkType.of("Loopback"))
        .accountRelationship(AccountRelationship.PEER)
        .isConnectionInitiator(true)
        .build();
      final AccountSettingsEntity accountSettingsEntity1 = new AccountSettingsEntity(accountSettings1);
      accountSettingsRepository.save(accountSettingsEntity1);
    }

    final AccountId accountId2 = AccountId.of(generateUuid());
    {
      final AccountSettings accountSettings2 = AccountSettings.builder()
        .accountId(accountId2)
        .assetCode("XRP")
        .assetScale(9)
        .linkType(LinkType.of("Loopback"))
        .accountRelationship(AccountRelationship.PEER)
        .isConnectionInitiator(true)
        .build();
      final AccountSettingsEntity accountSettingsEntity2 = new AccountSettingsEntity(accountSettings2);
      accountSettingsRepository.save(accountSettingsEntity2);
    }

    final Query query = entityManager.createNativeQuery(
      "UPDATE " + ACCOUNT_SETTINGS + " set " + ACCOUNT_ID + "='" + invalidAccountId + "' WHERE " + ACCOUNT_ID + "='"
        + accountId1.value() + "';"
    );
    assertThat(query.executeUpdate()).isEqualTo(1);
    // Clear the EM so that all reloads will occur properly in-sync with DB.
    entityManager.flush();
    entityManager.clear();

    assertThat(accountSettingsRepository.findByAccountId(accountId1)).isEmpty();
    assertThat(accountSettingsRepository.findAll()).hasSize(2);
    assertThat(accountSettingsRepository.findByAccountRelationshipIs(AccountRelationship.PEER).size()).isEqualTo(2);

    ///////////////
    // findByAccountRelationshipIsWithConversion
    assertThat(accountSettingsRepository.findByAccountRelationshipIsWithConversion(AccountRelationship.PEER).size())
      .isEqualTo(1);
    assertThat(
      accountSettingsRepository.findByAccountRelationshipIsWithConversion(AccountRelationship.PEER).stream().findFirst()
        .get().accountId()).isEqualTo(accountId2);

    ///////////////
    // findByAccountId(String)
    assertThat(accountSettingsRepository.findByAccountId(invalidAccountId)).isPresent();

    ///////////////
    // findByAccountIdWithConversion(AccountId)
    assertThat(accountSettingsRepository.findByAccountIdWithConversion(accountId1)).isEmpty();
    assertThat(accountSettingsRepository.findByAccountIdWithConversion(accountId2)).isNotEmpty();

    ///////////////
    //findFirstByAccountRelationshipWithConversion
    assertThat(accountSettingsRepository.findFirstByAccountRelationshipWithConversion(AccountRelationship.PEER).get()
      .accountId()).isEqualTo(accountId2);

    ///////////////
    //findAccountSettingsEntitiesByConnectionInitiatorIsTrueWithConversion
    assertThat(accountSettingsRepository.findAccountSettingsEntitiesByConnectionInitiatorIsTrueWithConversion().size())
      .isEqualTo(1);
  }

  @Test
  public void findAccountSettingsEntitiesByConnectionInitiatorIsTrue() {
    final AccountSettings accountSettings1 = AccountSettings.builder()
      .accountId(AccountId.of(generateUuid()))
      .assetCode("XRP")
      .assetScale(9)
      .isConnectionInitiator(true)
      .linkType(LinkType.of("Loopback"))
      .accountRelationship(AccountRelationship.PEER)
      .build();
    final AccountSettingsEntity accountSettingsEntity1 = new AccountSettingsEntity(accountSettings1);
    accountSettingsRepository.save(accountSettingsEntity1);

    ///////////////
    // When 1 of 1 is an initiator...
    ///////////////
    List<AccountSettingsEntity> initiatorAccounts =
      accountSettingsRepository.findAccountSettingsEntitiesByConnectionInitiatorIsTrue();
    assertThat(initiatorAccounts.size()).isOne();
    assertThat(initiatorAccounts.get(0)).isEqualTo(accountSettingsEntity1);
    assertThat(initiatorAccounts.contains(accountSettingsEntity1)).isTrue();

    ///////////////
    // When 1 of 2 is an initiator...
    ///////////////
    final AccountSettings accountSettings2 = AccountSettings.builder()
      .accountId(AccountId.of(generateUuid()))
      .assetCode("XRP")
      .assetScale(9)
      .isConnectionInitiator(false)
      .linkType(LinkType.of("Loopback"))
      .accountRelationship(AccountRelationship.CHILD)
      .build();
    final AccountSettingsEntity accountSettingsEntity2 = new AccountSettingsEntity(accountSettings2);
    accountSettingsRepository.save(accountSettingsEntity2);

    initiatorAccounts = accountSettingsRepository.findAccountSettingsEntitiesByConnectionInitiatorIsTrue();
    assertThat(initiatorAccounts.size()).isOne();
    assertThat(initiatorAccounts.get(0)).isEqualTo(accountSettingsEntity1);
    assertThat(initiatorAccounts.contains(accountSettingsEntity1)).isTrue();
    assertThat(initiatorAccounts.contains(accountSettingsEntity2)).isFalse();

    ///////////////
    // When 2 of 3 are initiators...
    ///////////////
    final AccountSettings accountSettings3 = AccountSettings.builder()
      .accountId(AccountId.of(generateUuid()))
      .assetCode("XRP")
      .assetScale(9)
      .isConnectionInitiator(true)
      .linkType(LinkType.of("Loopback"))
      .accountRelationship(AccountRelationship.CHILD)
      .build();
    final AccountSettingsEntity accountSettingsEntity3 = new AccountSettingsEntity(accountSettings3);
    accountSettingsRepository.save(accountSettingsEntity3);

    initiatorAccounts = accountSettingsRepository.findAccountSettingsEntitiesByConnectionInitiatorIsTrue();
    assertThat(initiatorAccounts.size()).isEqualTo(2);
    assertThat(initiatorAccounts.contains(accountSettingsEntity1)).isTrue();
    assertThat(initiatorAccounts.contains(accountSettingsEntity2)).isFalse();
    assertThat(initiatorAccounts.contains(accountSettingsEntity3)).isTrue();
  }

  @Test
  public void findAccountSettingsEntitiesByConnectionInitiatorIsTrueWithConversion() {
    final AccountSettings accountSettings1 = AccountSettings.builder()
      .accountId(AccountId.of(generateUuid()))
      .assetCode("XRP")
      .assetScale(9)
      .isConnectionInitiator(true)
      .linkType(LinkType.of("Loopback"))
      .accountRelationship(AccountRelationship.PEER)
      .build();
    final AccountSettingsEntity accountSettingsEntity1 = new AccountSettingsEntity(accountSettings1);
    accountSettingsRepository.save(accountSettingsEntity1);

    ///////////////
    // When 1 of 1 is an initiator...
    ///////////////
    List<AccountSettings> initiatorAccounts =
      accountSettingsRepository.findAccountSettingsEntitiesByConnectionInitiatorIsTrueWithConversion();
    assertThat(initiatorAccounts.size()).isOne();
    assertThat(initiatorAccounts.get(0)).isEqualTo(accountSettings1);
    assertThat(initiatorAccounts.contains(accountSettings1)).isTrue();

    ///////////////
    // When 1 of 2 is an initiator...
    ///////////////
    final AccountSettings accountSettings2 = AccountSettings.builder()
      .accountId(AccountId.of(generateUuid()))
      .assetCode("XRP")
      .assetScale(9)
      .isConnectionInitiator(false)
      .linkType(LinkType.of("Loopback"))
      .accountRelationship(AccountRelationship.CHILD)
      .build();
    final AccountSettingsEntity accountSettingsEntity2 = new AccountSettingsEntity(accountSettings2);
    accountSettingsRepository.save(accountSettingsEntity2);

    initiatorAccounts =
      accountSettingsRepository.findAccountSettingsEntitiesByConnectionInitiatorIsTrueWithConversion();
    assertThat(initiatorAccounts.size()).isOne();
    assertThat(initiatorAccounts.get(0)).isEqualTo(accountSettings1);
    assertThat(initiatorAccounts.contains(accountSettings1)).isTrue();
    assertThat(initiatorAccounts.contains(accountSettings2)).isFalse();

    ///////////////
    // When 2 of 3 are initiators...
    ///////////////
    final AccountSettings accountSettings3 = AccountSettings.builder()
      .accountId(AccountId.of(generateUuid()))
      .assetCode("XRP")
      .assetScale(9)
      .isConnectionInitiator(true)
      .linkType(LinkType.of("Loopback"))
      .accountRelationship(AccountRelationship.CHILD)
      .build();
    final AccountSettingsEntity accountSettingsEntity3 = new AccountSettingsEntity(accountSettings3);
    accountSettingsRepository.save(accountSettingsEntity3);

    initiatorAccounts =
      accountSettingsRepository.findAccountSettingsEntitiesByConnectionInitiatorIsTrueWithConversion();
    assertThat(initiatorAccounts.size()).isEqualTo(2);
    assertThat(initiatorAccounts.contains(accountSettings1)).isTrue();
    assertThat(initiatorAccounts.contains(accountSettings2)).isFalse();
    assertThat(initiatorAccounts.contains(accountSettings3)).isTrue();
  }

  @Test
  public void findBySettlementEngineAccountIdWithConversion() {
    final SettlementEngineAccountId settlementEngineAccountId =
      SettlementEngineAccountId.of(generateUuid());

    final AccountSettings accountSettings1 = AccountSettings.builder()
      .accountId(AccountId.of(generateUuid()))
      .assetCode("XRP")
      .assetScale(9)
      .linkType(LinkType.of("Loopback"))
      .accountRelationship(AccountRelationship.PEER)
      .settlementEngineDetails(
        SettlementEngineDetails.builder()
          .baseUrl(HttpUrl.parse("https://example.com"))
          .settlementEngineAccountId(settlementEngineAccountId)
          .build()
      )
      .build();
    final AccountSettingsEntity accountSettingsEntity = new AccountSettingsEntity(accountSettings1);
    accountSettingsRepository.save(accountSettingsEntity);

    Optional<AccountSettings> actual = accountSettingsRepository
      .findBySettlementEngineAccountIdWithConversion(settlementEngineAccountId);
    assertThat(actual.isPresent()).isTrue();

    assertThat(actual.get()).isEqualTo(accountSettings1);
  }

  @Test
  public void whenFindByAccountRelationshipWithConversion() {
    final AccountSettings accountSettings1 = AccountSettings.builder()
      .accountId(AccountId.of(generateUuid()))
      .assetCode("XRP")
      .assetScale(9)
      .linkType(LinkType.of("Loopback"))
      .accountRelationship(AccountRelationship.PEER)
      .build();
    final AccountSettingsEntity accountSettingsEntity1 = new AccountSettingsEntity(accountSettings1);
    accountSettingsRepository.save(accountSettingsEntity1);
    assertThat(
      accountSettingsRepository.findFirstByAccountRelationshipWithConversion(AccountRelationship.PARENT).isPresent())
      .isFalse();
    assertThat(
      accountSettingsRepository.findFirstByAccountRelationshipWithConversion(AccountRelationship.CHILD).isPresent())
      .isFalse();
    assertThat(
      accountSettingsRepository.findFirstByAccountRelationshipWithConversion(AccountRelationship.PEER).isPresent())
      .isTrue();

    final AccountSettings accountSettings1b = AccountSettings.builder()
      .accountId(AccountId.of(generateUuid()))
      .assetCode("XRP")
      .assetScale(9)
      .linkType(LinkType.of("Loopback"))
      .accountRelationship(AccountRelationship.CHILD)
      .build();
    final AccountSettingsEntity accountSettingsEntity1b = new AccountSettingsEntity(accountSettings1b);
    accountSettingsRepository.save(accountSettingsEntity1b);
    assertThat(
      accountSettingsRepository.findFirstByAccountRelationshipWithConversion(AccountRelationship.PARENT).isPresent())
      .isFalse();
    assertThat(
      accountSettingsRepository.findFirstByAccountRelationshipWithConversion(AccountRelationship.CHILD).isPresent())
      .isTrue();
    assertThat(
      accountSettingsRepository.findFirstByAccountRelationshipWithConversion(AccountRelationship.PEER).isPresent())
      .isTrue();

    final AccountSettings accountSettings2 = AccountSettings.builder()
      .accountId(AccountId.of(generateUuid()))
      .assetCode("XRP")
      .assetScale(9)
      .linkType(LinkType.of("Loopback"))
      .accountRelationship(AccountRelationship.PARENT)
      .build();
    final AccountSettingsEntity accountSettingsEntity2 = new AccountSettingsEntity(accountSettings2);
    accountSettingsRepository.save(accountSettingsEntity2);
    assertThat(accountSettingsRepository.findFirstByAccountRelationshipWithConversion(AccountRelationship.PARENT).get())
      .isEqualTo(accountSettings2); // Always finds the first `PARENT`
    assertThat(
      accountSettingsRepository.findFirstByAccountRelationshipWithConversion(AccountRelationship.CHILD).isPresent())
      .isTrue();
    assertThat(
      accountSettingsRepository.findFirstByAccountRelationshipWithConversion(AccountRelationship.PEER).isPresent())
      .isTrue();

    final AccountSettings accountSettings3 = AccountSettings.builder()
      .accountId(AccountId.of(generateUuid()))
      .assetCode("XRP")
      .assetScale(9)
      .linkType(LinkType.of("Loopback"))
      .accountRelationship(AccountRelationship.PARENT)
      .build();
    final AccountSettingsEntity accountSettingsEntity3 = new AccountSettingsEntity(accountSettings3);
    accountSettingsRepository.save(accountSettingsEntity3);
    assertThat(accountSettingsRepository.findFirstByAccountRelationshipWithConversion(AccountRelationship.PARENT).get())
      .isEqualTo(accountSettings2); // Always finds the first `PARENT`
    assertThat(
      accountSettingsRepository.findFirstByAccountRelationshipWithConversion(AccountRelationship.CHILD).isPresent())
      .isTrue();
    assertThat(
      accountSettingsRepository.findFirstByAccountRelationshipWithConversion(AccountRelationship.PEER).isPresent())
      .isTrue();

    final AccountSettings accountSettings4 = AccountSettings.builder()
      .accountId(AccountId.of(generateUuid()))
      .assetCode("XRP")
      .assetScale(9)
      .linkType(LinkType.of("Loopback"))
      .accountRelationship(AccountRelationship.PARENT)
      .build();
    final AccountSettingsEntity accountSettingsEntity4 = new AccountSettingsEntity(accountSettings4);
    accountSettingsRepository.save(accountSettingsEntity4);
    assertThat(accountSettingsRepository.findFirstByAccountRelationshipWithConversion(AccountRelationship.PARENT).get())
      .isEqualTo(accountSettings2); // Always finds the first `PARENT`
    assertThat(
      accountSettingsRepository.findFirstByAccountRelationshipWithConversion(AccountRelationship.CHILD).isPresent())
      .isTrue();
    assertThat(
      accountSettingsRepository.findFirstByAccountRelationshipWithConversion(AccountRelationship.PEER).isPresent())
      .isTrue();
  }

  @Test
  public void whenFindAllByAccountRelationshipWithConversion() {
    final AccountSettings accountSettings1 = AccountSettings.builder()
      .accountId(AccountId.of(generateUuid()))
      .assetCode("XRP")
      .assetScale(9)
      .linkType(LinkType.of("Loopback"))
      .accountRelationship(AccountRelationship.PEER)
      .build();
    final AccountSettingsEntity accountSettingsEntity1 = new AccountSettingsEntity(accountSettings1);
    accountSettingsRepository.save(accountSettingsEntity1);
    assertThat(accountSettingsRepository.findByAccountRelationshipIsWithConversion(AccountRelationship.PARENT).size())
      .isZero();
    assertThat(accountSettingsRepository.findByAccountRelationshipIsWithConversion(AccountRelationship.CHILD).size())
      .isZero();
    assertThat(accountSettingsRepository.findByAccountRelationshipIsWithConversion(AccountRelationship.PEER).size())
      .isOne();

    final AccountSettings accountSettings2 = AccountSettings.builder()
      .accountId(AccountId.of(generateUuid()))
      .assetCode("XRP")
      .assetScale(9)
      .linkType(LinkType.of("Loopback"))
      .accountRelationship(AccountRelationship.PARENT)
      .build();
    final AccountSettingsEntity accountSettingsEntity2 = new AccountSettingsEntity(accountSettings2);
    accountSettingsRepository.save(accountSettingsEntity2);
    assertThat(accountSettingsRepository.findByAccountRelationshipIsWithConversion(AccountRelationship.PARENT).size())
      .isOne();
    assertThat(accountSettingsRepository.findByAccountRelationshipIsWithConversion(AccountRelationship.CHILD).size())
      .isZero();
    assertThat(accountSettingsRepository.findByAccountRelationshipIsWithConversion(AccountRelationship.PEER).size())
      .isOne();

    final AccountSettings accountSettings3 = AccountSettings.builder()
      .accountId(AccountId.of(generateUuid()))
      .assetCode("XRP")
      .assetScale(9)
      .linkType(LinkType.of("Loopback"))
      .accountRelationship(AccountRelationship.CHILD)
      .build();
    final AccountSettingsEntity accountSettingsEntity3 = new AccountSettingsEntity(accountSettings3);
    accountSettingsRepository.save(accountSettingsEntity3);
    assertThat(accountSettingsRepository.findByAccountRelationshipIsWithConversion(AccountRelationship.PARENT).size())
      .isOne();
    assertThat(accountSettingsRepository.findByAccountRelationshipIsWithConversion(AccountRelationship.CHILD).size())
      .isOne();
    assertThat(accountSettingsRepository.findByAccountRelationshipIsWithConversion(AccountRelationship.PEER).size())
      .isOne();
  }

  //////////////////
  // Private Helpers
  //////////////////

  /**
   * Helper method to ensure two entities are equal by manually comparing each field. This is necessary because the
   * {@link AccountSettings#equals(Object)} does not compare the same fields as {@link
   * AccountSettingsEntity#equals(Object)} in order to support Hibernate.
   *
   * @param entity1
   * @param entity2
   *
   * @return {@code true} if the two objects have equivalent fields, {@code false} otherwise.
   */
  private void assertAllFieldsEqual(final AccountSettingsEntity entity1, final AccountSettingsEntity entity2) {
    Objects.requireNonNull(entity1);
    Objects.requireNonNull(entity2);

//    assertThat(entity1).isEqualToComparingFieldByField(entity2);
    assertThat(entity1.getAccountId()).isEqualTo(entity2.getAccountId());
    assertThat(entity1.getAccountRelationship()).isEqualTo(entity2.getAccountRelationship());
    assertThat(entity1.getDescription()).isEqualTo(entity2.getDescription());
    assertThat(entity1.getLinkType()).isEqualTo(entity2.getLinkType());
    assertThat(entity1.getAssetCode()).isEqualTo(entity2.getAssetCode());
    assertThat(entity1.getAssetScale()).isEqualTo(entity2.getAssetScale());
    assertThat(entity1.getIlpAddressSegment()).isEqualTo(entity2.getIlpAddressSegment());
    assertThat(entity1.getMaximumPacketAmount()).isEqualTo(entity2.getMaximumPacketAmount());

    // BalanceSettings
    assertThat(entity1.getBalanceSettings().getMinBalance()).isEqualTo(entity2.getBalanceSettings().getMinBalance());
    assertThat(entity1.getBalanceSettings().getSettleThreshold())
      .isEqualTo(entity2.getBalanceSettings().getSettleThreshold());
    assertThat(entity1.getBalanceSettings().getSettleTo()).isEqualTo(entity2.getBalanceSettings().getSettleTo());

    // RateLimitSettings
    assertThat(entity1.getRateLimitSettings().getMaxPacketsPerSecond())
      .isEqualTo(entity2.getRateLimitSettings().getMaxPacketsPerSecond());

    // SettlementEngineSettings
    if (entity1.settlementEngineDetails().isPresent()) {
      assertThat(entity1.settlementEngineDetails().isPresent())
        .isEqualTo(entity2.settlementEngineDetails().isPresent());
      assertThat(entity1.settlementEngineDetails().get().getBaseUrl())
        .isEqualTo(entity2.settlementEngineDetails().get().getBaseUrl());
      assertThat(entity1.settlementEngineDetails().get().getSettlementEngineAccountId())
        .isEqualTo(entity2.settlementEngineDetails().get().getSettlementEngineAccountId());
      assertThat(entity1.getCustomSettings()).isEqualTo(entity2.getCustomSettings());

    } else {
      assertThat(entity1.settlementEngineDetails()).isEqualTo(entity2.settlementEngineDetails());
    }
    // CustomSettings
    assertThat(entity1.getCustomSettings()).isEqualTo(entity2.getCustomSettings());
  }

  private String generateUuid() {
    return UUID.randomUUID().toString();
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
    ObjectMapper objectMapper() {
      return new ObjectMapper();
    }

    @Bean
    ConfigurableConversionService conversionService() {
      ConfigurableConversionService conversionService = new DefaultConversionService();
      conversionService.addConverter(rateLimitSettingsEntityConverter);
      conversionService.addConverter(accountBalanceSettingsEntityConverter);
      conversionService.addConverter(settlementEngineDetailsEntityConverter);
      conversionService.addConverter(accountSettingsConverter);
      return conversionService;
    }
  }
}
