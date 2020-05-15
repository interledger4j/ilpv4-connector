package org.interledger.connector.payments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.persistence.repositories.StreamPaymentsRepository;
import org.interledger.core.InterledgerAddress;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.data.domain.PageRequest;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class InDatabaseStreamPaymentManagerTest {

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private StreamPaymentsRepository mockRepo;

  @Mock
  private EventBus eventBus;

  private InDatabaseStreamPaymentManager transactionManager;
  private StreamPaymentFromEntityConverter streamPaymentFromEntityConverter = new StreamPaymentFromEntityConverter();
  private StreamPaymentToEntityConverter streamPaymentToEntityConverter = new StreamPaymentToEntityConverter();

  @Before
  public void setUp() {
    streamPaymentToEntityConverter = new StreamPaymentToEntityConverter();
    transactionManager = new InDatabaseStreamPaymentManager(mockRepo,
      streamPaymentFromEntityConverter,
      streamPaymentToEntityConverter,
      eventBus);
  }

  @Test
  public void findByAccountIdAndStreamPaymentId() {
    StreamPayment trx1 = transactionBuilder().build();
    when(mockRepo.findByAccountIdAndStreamPaymentId(trx1.accountId(), trx1.streamPaymentId()))
      .thenReturn(Optional.of(streamPaymentToEntityConverter.convert(trx1)));

    assertThat(transactionManager.findByAccountIdAndStreamPaymentId(trx1.accountId(), trx1.streamPaymentId()))
      .isEqualTo(Optional.of(trx1));
  }

  @Test
  public void findByAccountId() {
    StreamPayment trx1 = transactionBuilder().build();
    StreamPayment trx2 = transactionBuilder().build();
    when(mockRepo.findByAccountIdOrderByCreatedDateDesc(eq(trx1.accountId()), any()))
      .thenReturn(Lists.newArrayList(streamPaymentToEntityConverter.convert(trx1),
        streamPaymentToEntityConverter.convert(trx2)));

    assertThat(transactionManager.findByAccountId(trx1.accountId(), PageRequest.of(0, 100)))
      .isEqualTo(Lists.newArrayList(trx1, trx2));
  }

  @Test
  public void mergePending() {
    StreamPayment trx = transactionBuilder().build();
    transactionManager.merge(trx);
    verify(mockRepo, times(1)).upsertAmounts(streamPaymentToEntityConverter.convert(trx));
    verifyNoMoreInteractions(mockRepo);
  }

  @Test
  public void mergeAndUpdateStatus() {
    StreamPayment trx = transactionBuilder()
      .status(StreamPaymentStatus.CLOSED_BY_STREAM)
      .build();
    transactionManager.merge(trx);
    verify(mockRepo, times(1)).upsertAmounts(streamPaymentToEntityConverter.convert(trx));
    verify(mockRepo, times(1)).updateStatus(trx.accountId(), trx.streamPaymentId(), trx.status());
    verifyNoMoreInteractions(mockRepo);
  }

  @Test
  public void mergeAndUpdateSourceAddress() {
    InterledgerAddress source = InterledgerAddress.of("test.sender");
    StreamPayment trx = transactionBuilder()
      .sourceAddress(source)
      .build();
    transactionManager.merge(trx);
    verify(mockRepo, times(1)).upsertAmounts(streamPaymentToEntityConverter.convert(trx));
    verify(mockRepo, times(1)).updateSourceAddress(trx.accountId(), trx.streamPaymentId(), source.getValue());
    verifyNoMoreInteractions(mockRepo);
  }

  @Test
  public void mergeAndUpdateDeliveredDetails() {
    String assetCode = "XRP";
    short assetScale = 9;
    StreamPayment trx = transactionBuilder()
      .deliveredAssetScale(assetScale)
      .deliveredAssetCode(assetCode)
      .build();
    transactionManager.merge(trx);
    verify(mockRepo, times(1)).upsertAmounts(streamPaymentToEntityConverter.convert(trx));
    verify(mockRepo, times(1)).udpdateDeliveredDenomination(trx.accountId(), trx.streamPaymentId(), assetCode, assetScale);
    verifyNoMoreInteractions(mockRepo);
  }

  @Test
  public void mergeRejectsNegativePaymentReceived() {
    // payments received should not be negative (debit)
    expectedException.expect(IllegalArgumentException.class);
    transactionBuilder()
      .amount(BigInteger.valueOf(-1))
      .type(StreamPaymentType.PAYMENT_RECEIVED)
      .build();
  }

  @Test
  public void mergeRejectsPositivePaymentSent() {
    // payments sent should be a debit (negative amount)
    expectedException.expect(IllegalArgumentException.class);
    transactionBuilder()
      .amount(BigInteger.valueOf(1))
      .type(StreamPaymentType.PAYMENT_SENT)
      .build();
  }

  private ImmutableStreamPayment.Builder transactionBuilder() {
    return StreamPayment.builder()
      .accountId(AccountId.of("test"))
      .amount(BigInteger.valueOf(100))
      .assetCode("XRP")
      .assetScale((short) 1)
      .createdAt(Instant.now())
      .destinationAddress(InterledgerAddress.of("test.receiver"))
      .deliveredAmount(UnsignedLong.valueOf(100))
      .modifiedAt(Instant.now())
      .packetCount(1)
      .status(StreamPaymentStatus.PENDING)
      .streamPaymentId(UUID.randomUUID().toString())
      .type(StreamPaymentType.PAYMENT_RECEIVED);
  }


}