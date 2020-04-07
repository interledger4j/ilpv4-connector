package org.interledger.connector.persistence.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.persistence.config.ConnectorPersistenceConfig;
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
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
  private NamedParameterJdbcTemplate jdbcTemplate;

  @Test
  public void upsertSingleTransaction() {
    AccountId accountId = AccountId.of(generateUuid());
    String transactionId = generateUuid();

    final TransactionEntity entity1 = newEntity(accountId, transactionId, 10);

    transactionsRepository.upsertAmounts(entity1);

    final TransactionEntity loadedAccessTokenEntity =
      transactionsRepository.findByAccountIdAndReferenceId(accountId, transactionId).get();

    assertEqual(loadedAccessTokenEntity, entity1);
  }

  @Test
  public void updateStatus() {
    AccountId accountId = AccountId.of(generateUuid());
    String transactionId = generateUuid();

    final TransactionEntity entity1 = newEntity(accountId, transactionId, 10);

    transactionsRepository.upsertAmounts(entity1);

    transactionsRepository.updateStatus(accountId, transactionId, "CLOSED_BY_SENDER");

    TransactionEntity transaction1 =
      transactionsRepository.findByAccountIdAndReferenceId(accountId, transactionId).get();
    assertThat(transaction1.getStatus()).isEqualTo("CLOSED_BY_SENDER");
  }

  @Test
  public void findByAccountIdPagination() {
    AccountId accountId = AccountId.of(generateUuid());
    int transactionCount = 125;
    for (int i = 0; i < transactionCount; i++) {
      transactionsRepository.upsertAmounts(newEntity(accountId, generateUuid(), 10));
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
  public void findByAccountIdWithWrongAccountIdReturnsEmpty() {
    AccountId accountId = AccountId.of(generateUuid());
    transactionsRepository.upsertAmounts(newEntity(accountId, generateUuid(), 10));

    AccountId wrongAccountId = AccountId.of(generateUuid());
    assertThat(transactionsRepository.findByAccountIdOrderByCreatedDateDesc(wrongAccountId, DEFAULT_PAGE)).isEmpty();
  }

  @Test
  public void findByReferenceIdWithWrongAccountIdReturnsEmpty() {
    AccountId accountId = AccountId.of(generateUuid());
    String referenceId = generateUuid();
    transactionsRepository.upsertAmounts(newEntity(accountId, referenceId, 10));

    AccountId wrongAccountId = AccountId.of(generateUuid());
    assertThat(transactionsRepository.findByAccountIdAndReferenceId(wrongAccountId, referenceId)).isEmpty();
  }

  @Test
  public void upsertMultipleTransactions() throws InterruptedException {
    AccountId accountId = AccountId.of(generateUuid());
    String transactionId = generateUuid();
    String transactionId2 = generateUuid();

    final TransactionEntity entity1 = newEntity(accountId, transactionId, 10);

    transactionsRepository.upsertAmounts(entity1);

    final TransactionEntity loadedAccessTokenEntity =
      transactionsRepository.findByAccountIdAndReferenceId(accountId, transactionId).get();

    assertEqual(loadedAccessTokenEntity, entity1);

    Thread.sleep(1000); // sleep to make it more predictable to

    final TransactionEntity entity2 = newEntity(accountId, transactionId, 20);
    transactionsRepository.upsertAmounts(entity2);

    assertThat(transactionsRepository.findByAccountIdOrderByCreatedDateDesc(accountId, DEFAULT_PAGE)).hasSize(1);
    Optional<TransactionEntity> transaction1 =
      transactionsRepository.findByAccountIdAndReferenceId(accountId, transactionId);

    assertThat(transaction1).isPresent();
    assertThat(transaction1.get().getPacketCount()).isEqualTo(2);
    assertThat(transaction1.get().getAmount()).isEqualTo(BigInteger.valueOf(30));

    transactionsRepository.updateStatus(accountId, transactionId, "CLOSED_BY_SENDER");

    assertThat(transactionsRepository.findByAccountIdOrderByCreatedDateDesc(accountId, DEFAULT_PAGE)).hasSize(1);

    transaction1 = transactionsRepository.findByAccountIdAndReferenceId(accountId, transactionId);
    assertThat(transaction1.get().getStatus()).isEqualTo("CLOSED_BY_SENDER");

    transactionsRepository.upsertAmounts(newEntity(accountId, transactionId2, 33));

    assertThat(transactionsRepository.findByAccountIdOrderByCreatedDateDesc(accountId, DEFAULT_PAGE)).hasSize(2);

    Optional<TransactionEntity> transaction1Again =
      transactionsRepository.findByAccountIdAndReferenceId(accountId, transactionId);
    Optional<TransactionEntity> transaction2 =
      transactionsRepository.findByAccountIdAndReferenceId(accountId, transactionId2);

    assertEqual(transaction1Again.get(), transaction1.get());

    assertThat(transaction2).isPresent();
    assertThat(transaction2.get().getAmount()).isEqualTo(BigInteger.valueOf(33));
    assertThat(transaction2.get().getPacketCount()).isEqualTo(1);
    assertThat(transaction2.get().getModifiedDate()).isEqualTo(transaction2.get().getCreatedDate());
  }

  private String generateUuid() {
    return UUID.randomUUID().toString();
  }

  private TransactionEntity newEntity(AccountId accountId, String referenceId, long amount) {
    TransactionEntity entity = new TransactionEntity();
    entity.setSourceAddress("test.foo.bar");
    entity.setAccountId(accountId);
    entity.setPacketCount(1);
    entity.setReferenceId(referenceId);
    entity.setAmount(BigInteger.valueOf(amount));
    entity.setAssetCode("XRP");
    entity.setAssetScale((short) 9);
    entity.setDestinationAddress("test." + referenceId);
    entity.setStatus("PENDING");
    entity.setType("PAYMENT_RECEIVED");
    return entity;
  }

  private void assertEqual(TransactionEntity actual, TransactionEntity expected) {
    assertThat(actual).isEqualToIgnoringGivenFields(expected, "id", "createdDate", "modifiedDate");
  }

  @Configuration("application.yml")
  public static class TestPersistenceConfig {

    @Bean
    public ObjectMapper objectMapper() {
      return new ObjectMapper();
    }

    @Bean
    public ConfigurableConversionService conversionService() {
      return new DefaultConversionService();
    }
  }
}
