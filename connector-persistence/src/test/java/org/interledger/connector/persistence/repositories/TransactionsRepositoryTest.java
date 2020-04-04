package org.interledger.connector.persistence.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.persistence.config.ConnectorPersistenceConfig;
import org.interledger.connector.persistence.converters.AccessTokenEntityConverter;
import org.interledger.connector.persistence.entities.TransactionEntity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.persistence.EntityManager;

/**
 * Unit tests for {@link AccessTokensRepository}.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
  ConnectorPersistenceConfig.class, TransactionsRepositoryTest.TestPersistenceConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
@AutoConfigureEmbeddedDatabase
public class TransactionsRepositoryTest {

  public static final PageRequest DEFAULT_PAGE = PageRequest.of(0, 100);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Autowired
  private TransactionsRepository transactionsRepository;

  @Autowired
  EntityManager entityManager;

  @Test
  public void upsertSingleTransaction() {
    AccountId accountId = AccountId.of(generateUuid());
    String transactionId = generateUuid();

    final TransactionEntity entity1 = newEntity(accountId, transactionId, 10);

    final TransactionEntity savedEntity1 = save(entity1);
    assertEqual(savedEntity1, entity1);

    final TransactionEntity loadedAccessTokenEntity =
      transactionsRepository.findByReferenceId(savedEntity1.getReferenceId()).get();

    assertEqual(loadedAccessTokenEntity, savedEntity1);
  }

  @Test
  public void updateStatus() {
    AccountId accountId = AccountId.of(generateUuid());
    String transactionId = generateUuid();

    final TransactionEntity entity1 = newEntity(accountId, transactionId, 10);

    final TransactionEntity savedEntity1 = save(entity1);
    assertEqual(savedEntity1, entity1);

    savedEntity1.setStatus("CLOSED_BY_SENDER");
    save(savedEntity1);

    TransactionEntity transaction1 = transactionsRepository.findByReferenceId(transactionId).get();
    assertThat(transaction1.getStatus()).isEqualTo("CLOSED_BY_SENDER");
  }

  @Test
  public void findByAccountIdPagination() {
    AccountId accountId = AccountId.of(generateUuid());
    int transactionCount = 125;
    for (int i = 0; i < transactionCount; i++) {
      save(newEntity(accountId, generateUuid(), 10));
    }

    List<TransactionEntity> trx1to50 = transactionsRepository.findByAccountIdOrderByCreatedDateDesc(
      accountId, PageRequest.of(0, 50)
    );

    assertThat(trx1to50).hasSize(50);

    List<TransactionEntity> trx51To100 = transactionsRepository.findByAccountIdOrderByCreatedDateDesc(
      accountId, PageRequest.of(1, 50)
    );

    assertThat(trx51To100).hasSize(50);

    List<TransactionEntity> trx101To125 = transactionsRepository.findByAccountIdOrderByCreatedDateDesc(
      accountId, PageRequest.of(2, 50)
    );

    assertThat(trx101To125).hasSize(25);


    assertThat(ImmutableSet.builder().addAll(trx1to50).addAll(trx51To100).addAll(trx101To125).build())
      .hasSize(transactionCount);
  }

  @Test
  public void upsertMultipleTransactions() {
    AccountId accountId = AccountId.of(generateUuid());
    String transactionId = generateUuid();
    String transactionId2 = generateUuid();

    final TransactionEntity entity1 = newEntity(accountId, transactionId, 10);

    final TransactionEntity savedEntity1 = save(entity1);
    assertEqual(savedEntity1, entity1);

    final TransactionEntity loadedAccessTokenEntity =
      transactionsRepository.findByReferenceId(savedEntity1.getReferenceId()).get();

    assertEqual(loadedAccessTokenEntity, savedEntity1);

    final TransactionEntity entity2 = newEntity(accountId, transactionId, 20);
    save(entity2);

    assertThat(transactionsRepository.findByAccountIdOrderByCreatedDateDesc(accountId, DEFAULT_PAGE)).hasSize(1);
    Optional<TransactionEntity> transaction1 = transactionsRepository.findByReferenceId(transactionId);

    assertThat(transaction1).isPresent();
    assertThat(transaction1.get().getPacketCount()).isEqualTo(2);
    assertThat(transaction1.get().getAmount()).isEqualTo(BigInteger.valueOf(30));
    assertThat(transaction1.get().getModifiedDate()).isAfter(transaction1.get().getCreatedDate());

    transaction1.get().setStatus("CLOSED_BY_SENDER");
    save(transaction1.get());

    assertThat(transactionsRepository.findByAccountIdOrderByCreatedDateDesc(accountId, DEFAULT_PAGE)).hasSize(1);

    transaction1 = transactionsRepository.findByReferenceId(transactionId);
    assertThat(transaction1.get().getStatus()).isEqualTo("CLOSED_BY_SENDER");

    save(newEntity(accountId, transactionId2, 33));

    assertThat(transactionsRepository.findByAccountIdOrderByCreatedDateDesc(accountId, DEFAULT_PAGE)).hasSize(2);

    Optional<TransactionEntity> transaction1Again = transactionsRepository.findByReferenceId(transactionId);
    Optional<TransactionEntity> transaction2 = transactionsRepository.findByReferenceId(transactionId2);

    assertEqual(transaction1Again.get(), transaction1.get());

    assertThat(transaction2).isPresent();
    assertThat(transaction2.get().getAmount()).isEqualTo(BigInteger.valueOf(33));
    assertThat(transaction2.get().getPacketCount()).isEqualTo(1);
    assertThat(transaction2.get().getModifiedDate()).isEqualTo(transaction2.get().getCreatedDate());
  }

  private TransactionEntity save(TransactionEntity entity) {
    entityManager.clear();
    TransactionEntity result = transactionsRepository.save(entity);
    entityManager.flush();
    entityManager.refresh(result);
    entityManager.clear();

    return result;
  }

  private String generateUuid() {
    return UUID.randomUUID().toString();
  }

  private TransactionEntity newEntity(AccountId accountId, String referenceId, long amount) {
    TransactionEntity entity = new TransactionEntity();
    entity.setAccountId(accountId);
    entity.setPacketCount(1);
    entity.setReferenceId(referenceId);
    entity.setAmount(BigInteger.valueOf(amount));
    entity.setAssetCode("XRP");
    entity.setAssetScale((short) 9);
    entity.setDestinationAddress("test." + referenceId);
    entity.setStatus("PENDING");
    return entity;
  }

  private void assertEqual(TransactionEntity actual, TransactionEntity expected) {
    assertThat(actual).isEqualToIgnoringGivenFields(expected, "createdDate", "modifiedDate");
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
