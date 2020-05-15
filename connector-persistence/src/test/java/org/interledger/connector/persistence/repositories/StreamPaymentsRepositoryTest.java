package org.interledger.connector.persistence.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.payments.StreamPaymentStatus;
import org.interledger.connector.payments.StreamPaymentType;
import org.interledger.connector.persistence.config.ConnectorPersistenceConfig;
import org.interledger.connector.persistence.entities.StreamPaymentEntity;

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
  ConnectorPersistenceConfig.class, StreamPaymentsRepositoryTest.TestPersistenceConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
@AutoConfigureEmbeddedDatabase
public class StreamPaymentsRepositoryTest {

  public static final PageRequest DEFAULT_PAGE = PageRequest.of(0, 100);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Autowired
  private StreamPaymentsRepository streamPaymentsRepository;

  @Autowired
  private NamedParameterJdbcTemplate jdbcTemplate;

  @Test
  public void upsertSingleTransaction() {
    AccountId accountId = AccountId.of(generateUuid());
    String streamPaymentId = generateUuid();

    final StreamPaymentEntity entity1 = newEntity(accountId, streamPaymentId, 10);

    streamPaymentsRepository.upsertAmounts(entity1);

    final StreamPaymentEntity loadedAccessTokenEntity =
      streamPaymentsRepository.findByAccountIdAndStreamPaymentId(accountId, streamPaymentId).get();

    assertEqual(loadedAccessTokenEntity, entity1);
  }

  @Test
  public void upsertDestinationDetails() {
    AccountId accountId = AccountId.of(generateUuid());
    String streamPaymentId = generateUuid();
    short assetScale = 9;
    String assetCode = "XRP";

    for (int i = 0; i < 3; i++) {
      final StreamPaymentEntity entity1 = newEntity(accountId, streamPaymentId, 1);
      entity1.setAssetScale(assetScale);
      entity1.setAssetCode(assetCode);
      entity1.setDeliveredAmount(BigInteger.valueOf(10));
      streamPaymentsRepository.upsertAmounts(entity1);
    }
    final StreamPaymentEntity loadedAccessTokenEntity =
      streamPaymentsRepository.findByAccountIdAndStreamPaymentId(accountId, streamPaymentId).get();

    assertThat(loadedAccessTokenEntity.getAmount()).isEqualTo(BigInteger.valueOf(3));
    assertThat(loadedAccessTokenEntity.getDeliveredAmount()).isEqualTo(BigInteger.valueOf(30));
    assertThat(loadedAccessTokenEntity.getAssetCode()).isEqualTo(assetCode);
    assertThat(loadedAccessTokenEntity.getAssetScale()).isEqualTo(assetScale);
  }

  @Test
  public void updateSourceAddress() {
    AccountId accountId = AccountId.of(generateUuid());
    String streamPaymentId = generateUuid();
    String sourceAddress = "test.source";

    final StreamPaymentEntity entity1 = newEntity(accountId, streamPaymentId, 10);

    streamPaymentsRepository.upsertAmounts(entity1);

    streamPaymentsRepository.updateSourceAddress(accountId, streamPaymentId, sourceAddress);

    StreamPaymentEntity streamPayment1 =
      streamPaymentsRepository.findByAccountIdAndStreamPaymentId(accountId, streamPaymentId).get();
    assertThat(streamPayment1.getSourceAddress()).isEqualTo(sourceAddress);
  }

  @Test
  public void updateStatus() {
    AccountId accountId = AccountId.of(generateUuid());
    String streamPaymentId = generateUuid();

    final StreamPaymentEntity entity1 = newEntity(accountId, streamPaymentId, 10);

    streamPaymentsRepository.upsertAmounts(entity1);

    streamPaymentsRepository.updateStatus(accountId, streamPaymentId, StreamPaymentStatus.CLOSED_BY_STREAM);

    StreamPaymentEntity streamPayment1 =
      streamPaymentsRepository.findByAccountIdAndStreamPaymentId(accountId, streamPaymentId).get();
    assertThat(streamPayment1.getStatus()).isEqualTo(StreamPaymentStatus.CLOSED_BY_STREAM);
  }

  @Test
  public void findByAccountIdPagination() {
    AccountId accountId = AccountId.of(generateUuid());
    int streamPaymentCount = 125;
    for (int i = 0; i < streamPaymentCount; i++) {
      streamPaymentsRepository.upsertAmounts(newEntity(accountId, generateUuid(), 10));
    }

    List<StreamPaymentEntity> trx1to50 = streamPaymentsRepository.findByAccountIdOrderByCreatedDateDesc(
      accountId, PageRequest.of(0, 50)
    );

    assertThat(trx1to50).hasSize(50);

    List<StreamPaymentEntity> trx51To100 = streamPaymentsRepository.findByAccountIdOrderByCreatedDateDesc(
      accountId, PageRequest.of(1, 50)
    );

    assertThat(trx51To100).hasSize(50);

    List<StreamPaymentEntity> trx101To125 = streamPaymentsRepository.findByAccountIdOrderByCreatedDateDesc(
      accountId, PageRequest.of(2, 50)
    );

    assertThat(trx101To125).hasSize(25);


    assertThat(ImmutableSet.builder().addAll(trx1to50).addAll(trx51To100).addAll(trx101To125).build())
      .hasSize(streamPaymentCount);
  }

  @Test
  public void findByAccountIdCorrelationId() {
    AccountId accountId = AccountId.of(generateUuid());
    String invoice1 = "invoice1";
    String invoice2 = "invoice2";
    PageRequest pageRequest = PageRequest.of(0, 50);

    StreamPaymentEntity invoice1Payment1 = newEntity(accountId, generateUuid(), 10);
    invoice1Payment1.setCorrelationId(invoice1);
    streamPaymentsRepository.upsertAmounts(invoice1Payment1);

    StreamPaymentEntity invoice1Payment2 = newEntity(accountId, generateUuid(), 10);
    invoice1Payment2.setCorrelationId(invoice1);
    streamPaymentsRepository.upsertAmounts(invoice1Payment2);

    StreamPaymentEntity invoice2Payment1 = newEntity(accountId, generateUuid(), 10);
    invoice2Payment1.setCorrelationId(invoice2);
    streamPaymentsRepository.upsertAmounts(invoice2Payment1);

    List<StreamPaymentEntity> invoice1Payments =
      streamPaymentsRepository.findByAccountIdAndCorrelationIdOrderByCreatedDateDesc(accountId, invoice1, pageRequest);
    assertThat(invoice1Payments).hasSize(2);

    List<StreamPaymentEntity> invoice2Payments =
      streamPaymentsRepository.findByAccountIdAndCorrelationIdOrderByCreatedDateDesc(accountId, invoice2, pageRequest);
    assertThat(invoice2Payments).hasSize(1);
  }

  @Test
  public void findByAccountIdWithWrongAccountIdReturnsEmpty() {
    AccountId accountId = AccountId.of(generateUuid());
    streamPaymentsRepository.upsertAmounts(newEntity(accountId, generateUuid(), 10));

    AccountId wrongAccountId = AccountId.of(generateUuid());
    assertThat(streamPaymentsRepository.findByAccountIdOrderByCreatedDateDesc(wrongAccountId, DEFAULT_PAGE)).isEmpty();
  }

  @Test
  public void findByReferenceIdWithWrongAccountIdReturnsEmpty() {
    AccountId accountId = AccountId.of(generateUuid());
    String streamPaymentId = generateUuid();
    streamPaymentsRepository.upsertAmounts(newEntity(accountId, streamPaymentId, 10));

    AccountId wrongAccountId = AccountId.of(generateUuid());
    assertThat(streamPaymentsRepository.findByAccountIdAndStreamPaymentId(wrongAccountId, streamPaymentId)).isEmpty();
  }

  @Test
  public void upsertMultipleStreamPayments() {
    AccountId accountId = AccountId.of(generateUuid());
    String streamPaymentId = generateUuid();
    String streamPaymentId2 = generateUuid();

    final StreamPaymentEntity entity1 = newEntity(accountId, streamPaymentId, 10);

    streamPaymentsRepository.upsertAmounts(entity1);

    final StreamPaymentEntity loadedAccessTokenEntity =
      streamPaymentsRepository.findByAccountIdAndStreamPaymentId(accountId, streamPaymentId).get();

    assertEqual(loadedAccessTokenEntity, entity1);

    final StreamPaymentEntity entity2 = newEntity(accountId, streamPaymentId, 20);
    streamPaymentsRepository.upsertAmounts(entity2);

    assertThat(streamPaymentsRepository.findByAccountIdOrderByCreatedDateDesc(accountId, DEFAULT_PAGE)).hasSize(1);
    Optional<StreamPaymentEntity> streamPayment1 =
      streamPaymentsRepository.findByAccountIdAndStreamPaymentId(accountId, streamPaymentId);

    assertThat(streamPayment1).isPresent();
    assertThat(streamPayment1.get().getPacketCount()).isEqualTo(2);
    assertThat(streamPayment1.get().getAmount()).isEqualTo(BigInteger.valueOf(30));

    streamPaymentsRepository.updateStatus(accountId, streamPaymentId, StreamPaymentStatus.CLOSED_BY_STREAM);

    assertThat(streamPaymentsRepository.findByAccountIdOrderByCreatedDateDesc(accountId, DEFAULT_PAGE)).hasSize(1);

    streamPayment1 = streamPaymentsRepository.findByAccountIdAndStreamPaymentId(accountId, streamPaymentId);
    assertThat(streamPayment1.get().getStatus()).isEqualTo(StreamPaymentStatus.CLOSED_BY_STREAM);

    streamPaymentsRepository.upsertAmounts(newEntity(accountId, streamPaymentId2, 33));

    assertThat(streamPaymentsRepository.findByAccountIdOrderByCreatedDateDesc(accountId, DEFAULT_PAGE)).hasSize(2);

    Optional<StreamPaymentEntity> streamPayment1Again =
      streamPaymentsRepository.findByAccountIdAndStreamPaymentId(accountId, streamPaymentId);
    Optional<StreamPaymentEntity> streamPayment2 =
      streamPaymentsRepository.findByAccountIdAndStreamPaymentId(accountId, streamPaymentId2);

    assertEqual(streamPayment1Again.get(), streamPayment1.get());

    assertThat(streamPayment2).isPresent();
    assertThat(streamPayment2.get().getAmount()).isEqualTo(BigInteger.valueOf(33));
    assertThat(streamPayment2.get().getPacketCount()).isEqualTo(1);
    assertThat(streamPayment2.get().getModifiedDate()).isEqualTo(streamPayment2.get().getCreatedDate());
  }

  private String generateUuid() {
    return UUID.randomUUID().toString();
  }

  private StreamPaymentEntity newEntity(AccountId accountId, String streamPaymentId, long amount) {
    StreamPaymentEntity entity = new StreamPaymentEntity();
    entity.setSourceAddress("test.foo.bar");
    entity.setAccountId(accountId);
    entity.setCorrelationId("correlation is not causation");
    entity.setPacketCount(1);
    entity.setStreamPaymentId(streamPaymentId);
    entity.setAmount(BigInteger.valueOf(amount));
    entity.setExpectedAmount(BigInteger.valueOf(amount));
    entity.setAssetCode("XRP");
    entity.setAssetScale((short) 9);
    entity.setDestinationAddress("test." + streamPaymentId);
    entity.setStatus(StreamPaymentStatus.PENDING);
    entity.setType(StreamPaymentType.PAYMENT_RECEIVED);
    return entity;
  }

  private void assertEqual(StreamPaymentEntity actual, StreamPaymentEntity expected) {
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
