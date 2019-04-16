package org.interledger.ilpv4.connector.persistence.repositories;

import com.google.common.collect.Maps;
import org.interledger.connector.accounts.AccountBalanceSettings;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRateLimitSettings;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.link.LinkType;
import org.interledger.ilpv4.connector.persistence.config.ConnectorPersistenceConfig;
import org.interledger.ilpv4.connector.persistence.entities.AccountSettingsEntity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;

/**
 * Unit tests for {@link AccountSettingsRepository}.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ConnectorPersistenceConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
public class AccountSettingsRepositoryTest {

  @Autowired
  private AccountSettingsRepository accountSettingsRepository;

  @Test
  public void whenSaveAndLoadWithAllFieldsPopulated() {
    final Map<String, Object> customSettings = Maps.newHashMap();
    customSettings.put("address", "123 Main Street");
    customSettings.put("zipcode", 12345);

    final AccountSettings accountSettings = AccountSettings.builder()
      .accountId(AccountId.of(UUID.randomUUID().toString()))
      .description("description")
      .assetCode("XRP")
      .assetScale(9)
      .maximumPacketAmount(BigInteger.TEN)
      .linkType(LinkType.of("Loopback"))
      .isPreconfigured(true)
      .isInternal(true)
      .relationship(AccountRelationship.PEER)
      .rateLimitSettings(AccountRateLimitSettings.builder()
        .maxPacketsPerSecond(10)
        .build())
      .balanceSettings(AccountBalanceSettings.builder()
        .maxBalance(BigInteger.TEN)
        .minBalance(BigInteger.ZERO)
        .settleThreshold(BigInteger.valueOf(100L))
        .settleTo(BigInteger.valueOf(150L))
        .build())
      .ilpAddressSegment("foo")
      .customSettings(customSettings)
      .build();

    final AccountSettingsEntity accountSettingsEntity = new AccountSettingsEntity(accountSettings);
    assertThat(accountSettingsEntity.getId(), is(nullValue()));
    assertThat(accountSettingsEntity.getNaturalId().toString(), is(accountSettings.getAccountId().value()));
    assertAllFieldsEqual(accountSettingsEntity, accountSettings);

    // Equals methods are not the same, so verify this.
    assertThat(accountSettingsEntity, is(not(accountSettings)));

    final AccountSettingsEntity savedAccountSettingsEntity = accountSettingsRepository.save(accountSettingsEntity);
    assertThat(savedAccountSettingsEntity, is(accountSettingsEntity));
    assertThat(savedAccountSettingsEntity.getId() > 0, is(true));
    assertThat(savedAccountSettingsEntity.getNaturalId().toString(), is(accountSettings.getAccountId().value()));
    assertAllFieldsEqual(savedAccountSettingsEntity, accountSettings);

    final AccountSettingsEntity loadedAccountSettingsEntity =
      accountSettingsRepository.findById(savedAccountSettingsEntity.getId()).get();
    assertThat(loadedAccountSettingsEntity.getId() > 0, is(true));
    assertThat(loadedAccountSettingsEntity.getNaturalId().toString(), is(accountSettings.getAccountId().value()));
    assertAllFieldsEqual(loadedAccountSettingsEntity, accountSettings);

    final AccountSettingsEntity loadedAccountSettingsEntity2 =
      accountSettingsRepository.findByNaturalId(UUID.fromString(accountSettings.getAccountId().value())).get();
    assertThat(loadedAccountSettingsEntity2.getId(), is(loadedAccountSettingsEntity.getId()));
    assertThat(loadedAccountSettingsEntity2.getNaturalId(), is(loadedAccountSettingsEntity.getNaturalId()));
    assertAllFieldsEqual(loadedAccountSettingsEntity2, accountSettings);
  }

  @Test
  public void whenSaveAndLoadWithMinimalFieldsPopulated() {
    final Map<String, Object> customSettings = Maps.newHashMap();

    final AccountSettings accountSettings = AccountSettings.builder()
      .accountId(AccountId.of(UUID.randomUUID().toString()))
      .assetCode("XRP")
      .assetScale(9)
      .linkType(LinkType.of("Loopback"))
      .relationship(AccountRelationship.PEER)
      .build();

    final AccountSettingsEntity accountSettingsEntity = new AccountSettingsEntity(accountSettings);
    assertThat(accountSettingsEntity.getId(), is(nullValue()));
    assertThat(accountSettingsEntity.getNaturalId().toString(), is(accountSettings.getAccountId().value()));
    assertAllFieldsEqual(accountSettingsEntity, accountSettings);

    // Equals methods are not the same, so verify this.
    assertThat(accountSettingsEntity, is(not(accountSettings)));

    final AccountSettingsEntity savedAccountSettingsEntity = accountSettingsRepository.save(accountSettingsEntity);
    assertThat(savedAccountSettingsEntity, is(accountSettingsEntity));
    assertThat(savedAccountSettingsEntity.getId() > 0, is(true));
    assertThat(savedAccountSettingsEntity.getNaturalId().toString(), is(accountSettings.getAccountId().value()));
    assertAllFieldsEqual(savedAccountSettingsEntity, accountSettings);

    final AccountSettingsEntity loadedAccountSettingsEntity =
      accountSettingsRepository.findById(savedAccountSettingsEntity.getId()).get();
    assertThat(loadedAccountSettingsEntity.getId() > 0, is(true));
    assertThat(loadedAccountSettingsEntity.getNaturalId().toString(), is(accountSettings.getAccountId().value()));
    assertAllFieldsEqual(loadedAccountSettingsEntity, accountSettings);

    // Assert actual loaded values...
    assertThat(loadedAccountSettingsEntity.getRelationship(), is(AccountRelationship.PEER));
    assertThat(loadedAccountSettingsEntity.getLinkType(), is(LinkType.of("Loopback")));
    assertThat(loadedAccountSettingsEntity.getAssetCode(), is("XRP"));
    assertThat(loadedAccountSettingsEntity.getAssetScale(), is(9));
    assertThat(loadedAccountSettingsEntity.getCustomSettings().size(), is(0));
    assertThat(loadedAccountSettingsEntity.getMaximumPacketAmount().isPresent(), is(false));
    assertThat(loadedAccountSettingsEntity.getBalanceSettings().getMaxBalance().isPresent(), is(false));
    assertThat(loadedAccountSettingsEntity.getBalanceSettings().getMinBalance().isPresent(), is(false));
    assertThat(loadedAccountSettingsEntity.getBalanceSettings().getSettleThreshold().isPresent(), is(false));
    assertThat(loadedAccountSettingsEntity.getBalanceSettings().getSettleTo().isPresent(), is(false));
    assertThat(loadedAccountSettingsEntity.getRateLimitSettings().getMaxPacketsPerSecond().isPresent(), is(false));
    assertThat(loadedAccountSettingsEntity.getIlpAddressSegment().isPresent(), is(false));

    final AccountSettingsEntity loadedAccountSettingsEntity2 =
      accountSettingsRepository.findByNaturalId(UUID.fromString(accountSettings.getAccountId().value())).get();
    assertThat(loadedAccountSettingsEntity2.getId(), is(loadedAccountSettingsEntity.getId()));
    assertThat(loadedAccountSettingsEntity2.getNaturalId(), is(loadedAccountSettingsEntity.getNaturalId()));
    assertAllFieldsEqual(loadedAccountSettingsEntity2, accountSettings);

    assertThat(loadedAccountSettingsEntity.getRelationship(), is(AccountRelationship.PEER));
    assertThat(loadedAccountSettingsEntity.getLinkType(), is(LinkType.of("Loopback")));
    assertThat(loadedAccountSettingsEntity.getAssetCode(), is("XRP"));
    assertThat(loadedAccountSettingsEntity.getAssetScale(), is(9));
    assertThat(loadedAccountSettingsEntity.getCustomSettings().size(), is(0));
    assertThat(loadedAccountSettingsEntity.getMaximumPacketAmount().isPresent(), is(false));
    assertThat(loadedAccountSettingsEntity.getBalanceSettings().getMaxBalance().isPresent(), is(false));
    assertThat(loadedAccountSettingsEntity.getBalanceSettings().getMinBalance().isPresent(), is(false));
    assertThat(loadedAccountSettingsEntity.getBalanceSettings().getSettleThreshold().isPresent(), is(false));
    assertThat(loadedAccountSettingsEntity.getBalanceSettings().getSettleTo().isPresent(), is(false));
    assertThat(loadedAccountSettingsEntity.getRateLimitSettings().getMaxPacketsPerSecond().isPresent(), is(false));
    assertThat(loadedAccountSettingsEntity.getIlpAddressSegment().isPresent(), is(false));
  }

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
  private void assertAllFieldsEqual(final AccountSettings entity1, final AccountSettings entity2) {
    Objects.requireNonNull(entity1);
    Objects.requireNonNull(entity2);

    assertThat(entity1.getAccountId(), is(entity2.getAccountId()));
    assertThat(entity1.getDescription(), is(entity2.getDescription()));
    assertThat(entity1.getAssetCode(), is(entity2.getAssetCode()));
    assertThat(entity1.getAssetScale(), is(entity2.getAssetScale()));
    assertThat(entity1.getBalanceSettings().getMaxBalance(), is(entity2.getBalanceSettings().getMaxBalance()));
    assertThat(entity1.getBalanceSettings().getMinBalance(), is(entity2.getBalanceSettings().getMinBalance()));
    assertThat(entity1.getBalanceSettings().getSettleThreshold(),
      is(entity2.getBalanceSettings().getSettleThreshold()));
    assertThat(entity1.getBalanceSettings().getSettleTo(), is(entity2.getBalanceSettings().getSettleTo()));
    assertThat(entity1.getRateLimitSettings().getMaxPacketsPerSecond(),
      is(entity2.getRateLimitSettings().getMaxPacketsPerSecond()));
    assertThat(entity1.getLinkType(), is(entity2.getLinkType()));
    assertThat(entity1.getIlpAddressSegment(), is(entity2.getIlpAddressSegment()));
    assertThat(entity1.getRelationship(), is(entity2.getRelationship()));
    assertThat(entity1.getMaximumPacketAmount(), is(entity2.getMaximumPacketAmount()));
    assertThat(entity1.getCustomSettings(), is(entity2.getCustomSettings()));
  }

  //  @Configuration("application.yml")
  //  @EnableJpaRepositories(basePackages = "org.interledger.ilpv4.connector.persistence.repositories")
  //@EnableRedisRepositories(basePackages = "org.interledger.ilpv4.connector.persistence.repository")
  public static class PersistenceConfig {


  }
}