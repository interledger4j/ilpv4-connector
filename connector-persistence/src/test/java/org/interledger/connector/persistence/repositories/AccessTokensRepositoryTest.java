package org.interledger.connector.persistence.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.accounts.AccessToken;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.persistence.config.ConnectorPersistenceConfig;
import org.interledger.connector.persistence.converters.AccessTokenEntityConverter;
import org.interledger.connector.persistence.entities.AccessTokenEntity;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Unit tests for {@link AccessTokensRepository}.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
  ConnectorPersistenceConfig.class, AccessTokensRepositoryTest.TestPersistenceConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
@AutoConfigureEmbeddedDatabase
public class AccessTokensRepositoryTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Autowired
  private AccessTokensRepository accessTokensRepository;

  @Test
  public void whenSaveAndLoadWithAllFieldsPopulated() {
    AccountId accountId = AccountId.of(generateUuid());

    final AccessTokenEntity accessTokenEntity = newEntity(accountId);

    final AccessTokenEntity savedAccessTokenEntity = accessTokensRepository.save(accessTokenEntity);
    assertAllFieldsEqual(savedAccessTokenEntity, accessTokenEntity);

    final AccessTokenEntity loadedAccessTokenEntity =
      accessTokensRepository.findById(savedAccessTokenEntity.getId()).get();
    assertAllFieldsEqual(loadedAccessTokenEntity, savedAccessTokenEntity);
  }

  @Test
  public void findByIdWhenNonExistent() {
    assertThat(
      accessTokensRepository.findById(1L)
        .isPresent()
    ).isFalse();
  }

  @Test
  public void findByIdWithConversion() {
    final AccountId accountId = AccountId.of(generateUuid());
    final AccessTokenEntity toSave = newEntity(accountId);

    AccessTokenEntity saved = accessTokensRepository.save(toSave);

    Optional<AccessToken> loadedAccessToken =
      accessTokensRepository.withConversion(
        accessTokensRepository.findByAccountIdAndId(accountId, saved.getId()));
    assertThat(loadedAccessToken.isPresent()).isTrue();

    AccessToken expected = AccessToken.builder()
      .id(saved.getId())
      .encryptedToken(toSave.getEncryptedToken())
      .accountId(accountId)
      .createdAt(saved.getCreatedDate())
      .build();

    assertThat(loadedAccessToken.get()).isEqualTo(expected);
  }

  @Test
  public void deleteById() {
    final AccountId accountId = AccountId.of(generateUuid());

    AccessTokenEntity saved1 = accessTokensRepository.save(newEntity(accountId));
    AccessTokenEntity saved2 = accessTokensRepository.save(newEntity(accountId));
    AccessTokenEntity saved3 = accessTokensRepository.save(newEntity(accountId));

    assertThat(accessTokensRepository.findByAccountIdAndId(accountId, saved1.getId())).isPresent();
    assertThat(accessTokensRepository.findByAccountIdAndId(accountId, saved2.getId())).isPresent();
    assertThat(accessTokensRepository.findByAccountIdAndId(accountId, saved3.getId())).isPresent();

    accessTokensRepository.deleteByAccountIdAndId(accountId, saved2.getId());

    assertThat(accessTokensRepository.findByAccountIdAndId(accountId, saved1.getId())).isPresent();
    assertThat(accessTokensRepository.findByAccountIdAndId(accountId, saved2.getId())).isNotPresent();
    assertThat(accessTokensRepository.findByAccountIdAndId(accountId, saved3.getId())).isPresent();
  }

  @Test
  public void deleteByAccountId() {
    final AccountId accountId = AccountId.of(generateUuid());
    final AccountId otherAccountId = AccountId.of(generateUuid());

    AccessTokenEntity saved1 = accessTokensRepository.save(newEntity(accountId));
    AccessTokenEntity saved2 = accessTokensRepository.save(newEntity(accountId));
    AccessTokenEntity saved3 = accessTokensRepository.save(newEntity(accountId));
    AccessTokenEntity other = accessTokensRepository.save(newEntity(otherAccountId));

    assertThat(accessTokensRepository.findByAccountIdAndId(accountId, saved1.getId())).isPresent();
    assertThat(accessTokensRepository.findByAccountIdAndId(accountId, saved2.getId())).isPresent();
    assertThat(accessTokensRepository.findByAccountIdAndId(accountId, saved3.getId())).isPresent();
    assertThat(accessTokensRepository.findByAccountIdAndId(otherAccountId, other.getId())).isPresent();

    accessTokensRepository.deleteByAccountId(accountId);

    assertThat(accessTokensRepository.findByAccountIdAndId(accountId, saved1.getId())).isNotPresent();
    assertThat(accessTokensRepository.findByAccountIdAndId(accountId, saved2.getId())).isNotPresent();
    assertThat(accessTokensRepository.findByAccountIdAndId(accountId, saved3.getId())).isNotPresent();
    assertThat(accessTokensRepository.findByAccountIdAndId(otherAccountId, other.getId())).isPresent();
  }

  //////////////////
  // Private Helpers
  //////////////////

  /**
   * Helper method to ensure two entities are equal by manually comparing each field. This is necessary because the
   * {@link AccessToken#equals(Object)} does not compare the same fields as {@link
   * AccessTokenEntity#equals(Object)} in order to support Hibernate.
   *
   * @param entity1
   * @param entity2
   *
   * @return {@code true} if the two objects have equivalent fields, {@code false} otherwise.
   */
  private void assertAllFieldsEqual(final AccessTokenEntity entity1, final AccessTokenEntity entity2) {
    Objects.requireNonNull(entity1);
    Objects.requireNonNull(entity2);

    assertThat(entity1.getAccountId()).isEqualTo(entity2.getAccountId());
    assertThat(entity1.getEncryptedToken()).isEqualTo(entity2.getEncryptedToken());
    assertThat(entity1.getCreatedDate()).isEqualTo(entity2.getCreatedDate());
    assertThat(entity1.getId()).isEqualTo(entity2.getId());
  }

  private String generateUuid() {
    return UUID.randomUUID().toString();
  }

  private AccessTokenEntity newEntity(AccountId accountId) {
    AccessTokenEntity entity = new AccessTokenEntity();
    entity.setAccountId(accountId);
    entity.setEncryptedToken(UUID.randomUUID().toString());
    return entity;
  }


  @Configuration("application.yml")
  public static class TestPersistenceConfig {

    ////////////////////////
    // SpringConverters
    ////////////////////////

    @Autowired
    private AccessTokenEntityConverter accessTokensEntityConverter;

    @Bean
    public ObjectMapper objectMapper() {
      return new ObjectMapper();
    }

    @Bean
    public ConfigurableConversionService conversionService() {
      ConfigurableConversionService conversionService = new DefaultConversionService();
      conversionService.addConverter(accessTokensEntityConverter);
      return conversionService;
    }
  }
}
