package org.interledger.connector.transactions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.persistence.repositories.TransactionsRepository;
import org.interledger.core.InterledgerAddress;

import com.google.common.collect.Lists;
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

public class InDatabasePaymentTransactionManagerTest {

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private TransactionsRepository mockRepo;

  private InDatabasePaymentTransactionManager transactionManager;
  private TransactionFromEntityConverter transactionFromEntityConverter = new TransactionFromEntityConverter();
  private TransactionToEntityConverter transactionToEntityConverter = new TransactionToEntityConverter();

  @Before
  public void setUp() {
    transactionToEntityConverter = new TransactionToEntityConverter();
    transactionManager = new InDatabasePaymentTransactionManager(mockRepo,
      transactionFromEntityConverter,
      transactionToEntityConverter);
  }

  @Test
  public void findByAccountIdAndTransactionId() {
    Transaction trx1 = transactionBuilder().build();
    when(mockRepo.findByAccountIdAndTransactionId(trx1.accountId(), trx1.transactionId()))
      .thenReturn(Optional.of(transactionToEntityConverter.convert(trx1)));

    assertThat(transactionManager.findByAccountIdAndTransactionId(trx1.accountId(), trx1.transactionId()))
      .isEqualTo(Optional.of(trx1));
  }

  @Test
  public void findByAccountId() {
    Transaction trx1 = transactionBuilder().build();
    Transaction trx2 = transactionBuilder().build();
    when(mockRepo.findByAccountIdOrderByCreatedDateDesc(eq(trx1.accountId()), any()))
      .thenReturn(Lists.newArrayList(transactionToEntityConverter.convert(trx1),
        transactionToEntityConverter.convert(trx2)));

    assertThat(transactionManager.findByAccountId(trx1.accountId(), PageRequest.of(0, 100)))
      .isEqualTo(Lists.newArrayList(trx1, trx2));
  }

  @Test
  public void mergePending() {
    Transaction trx = transactionBuilder().build();
    transactionManager.merge(trx);
    verify(mockRepo, times(1)).upsertAmounts(transactionToEntityConverter.convert(trx));
    verifyNoMoreInteractions(mockRepo);
  }

  @Test
  public void mergeAndUpdateStatus() {
    Transaction trx = transactionBuilder()
      .status(TransactionStatus.CLOSED_BY_STREAM)
      .build();
    transactionManager.merge(trx);
    verify(mockRepo, times(1)).upsertAmounts(transactionToEntityConverter.convert(trx));
    verify(mockRepo, times(1)).updateStatus(trx.accountId(), trx.transactionId(), trx.status());
    verifyNoMoreInteractions(mockRepo);
  }

  @Test
  public void mergeAndUpdateSourceAddress() {
    InterledgerAddress source = InterledgerAddress.of("test.sender");
    Transaction trx = transactionBuilder()
      .sourceAddress(source)
      .build();
    transactionManager.merge(trx);
    verify(mockRepo, times(1)).upsertAmounts(transactionToEntityConverter.convert(trx));
    verify(mockRepo, times(1)).updateSourceAddress(trx.accountId(), trx.transactionId(), source.getValue());
    verifyNoMoreInteractions(mockRepo);
  }

  @Test
  public void mergeRejectsNegativePaymentReceived() {
    // payments received should not be negative (debit)
    Transaction trx = transactionBuilder()
      .amount(BigInteger.valueOf(-1))
      .type(TransactionType.PAYMENT_RECEIVED)
      .build();

    expectedException.expect(IllegalArgumentException.class);
    transactionManager.merge(trx);
  }

  @Test
  public void mergeRejectsPostivePaymentSent() {
    // payments sent should be a debit (negative amount)
    Transaction trx = transactionBuilder()
      .amount(BigInteger.valueOf(1))
      .type(TransactionType.PAYMENT_SENT)
      .build();

    expectedException.expect(IllegalArgumentException.class);
    transactionManager.merge(trx);
  }

  private ImmutableTransaction.Builder transactionBuilder() {
    return Transaction.builder()
      .accountId(AccountId.of("test"))
      .amount(BigInteger.valueOf(100))
      .assetCode("XRP")
      .assetScale((short) 1)
      .createdAt(Instant.now())
      .destinationAddress(InterledgerAddress.of("test.receiver"))
      .modifiedAt(Instant.now())
      .packetCount(1)
      .status(TransactionStatus.PENDING)
      .transactionId(UUID.randomUUID().toString())
      .type(TransactionType.PAYMENT_RECEIVED);
  }


}