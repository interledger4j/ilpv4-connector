package org.interledger.connector.payments;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerAddress;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.PageRequest;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class InMemoryStreamPaymentManagerTest {

  public static final PageRequest DEFAULT_PAGE = PageRequest.of(0, 100);

  private InMemoryStreamPaymentnManager paymentTransactionManager;

  @Before
  public void setUp() {
    paymentTransactionManager = new InMemoryStreamPaymentnManager();
  }

  @Test
  public void findByAccountIdPagination() {
    AccountId accountId = AccountId.of(generateUuid());
    int transactionCount = 125;
    for (int i = 0; i < transactionCount; i++) {
      paymentTransactionManager.merge(newTransaction(accountId, generateUuid(), 10));
    }

    List<StreamPayment> trx1to50 = paymentTransactionManager.findByAccountId(
      accountId, PageRequest.of(0, 50)
    );

    assertThat(trx1to50).hasSize(50);

    List<StreamPayment> trx51To100 = paymentTransactionManager.findByAccountId(
      accountId, PageRequest.of(1, 50)
    );

    assertThat(trx51To100).hasSize(50);

    List<StreamPayment> trx101To125 = paymentTransactionManager.findByAccountId(
      accountId, PageRequest.of(2, 50)
    );

    assertThat(trx101To125).hasSize(25);


    assertThat(ImmutableSet.builder().addAll(trx1to50).addAll(trx51To100).addAll(trx101To125).build())
      .hasSize(transactionCount);
  }

  @Test
  public void merge() {
    AccountId accountId = AccountId.of(generateUuid());
    String streamPaymentId = generateUuid();
    String streamPaymentId2 = generateUuid();

    final StreamPayment entity1 = newTransaction(accountId, streamPaymentId, 10);

    paymentTransactionManager.merge(entity1);

    final StreamPayment loadedAccessTokenEntity =
      paymentTransactionManager.findByAccountIdAndStreamPaymentId(accountId, streamPaymentId).get();

    assertEqual(loadedAccessTokenEntity, entity1);

    final StreamPayment entity2 = newTransaction(accountId, streamPaymentId, 20);
    paymentTransactionManager.merge(entity2);

    assertThat(paymentTransactionManager.findByAccountId(accountId, DEFAULT_PAGE)).hasSize(1);
    Optional<StreamPayment> transaction1 =
      paymentTransactionManager.findByAccountIdAndStreamPaymentId(accountId, streamPaymentId);

    assertThat(transaction1).isPresent();
    assertThat(transaction1.get().packetCount()).isEqualTo(2);
    assertThat(transaction1.get().amount()).isEqualTo(BigInteger.valueOf(30));

    paymentTransactionManager.merge(newTransaction(accountId, streamPaymentId2, 33));

    assertThat(paymentTransactionManager.findByAccountId(accountId, DEFAULT_PAGE)).hasSize(2);

    Optional<StreamPayment> transaction1Again =
      paymentTransactionManager.findByAccountIdAndStreamPaymentId(accountId, streamPaymentId);
    Optional<StreamPayment> transaction2 =
      paymentTransactionManager.findByAccountIdAndStreamPaymentId(accountId, streamPaymentId2);

    assertEqual(transaction1Again.get(), transaction1.get());

    assertThat(transaction2).isPresent();
    assertThat(transaction2.get().amount()).isEqualTo(BigInteger.valueOf(33));
    assertThat(transaction2.get().packetCount()).isEqualTo(1);
  }

  private void assertEqual(StreamPayment loadedAccessTokenEntity, StreamPayment entity1) {
    assertThat(loadedAccessTokenEntity).isEqualTo(entity1);
  }

  private StreamPayment newTransaction(AccountId accountId, String streamPaymentId, long amount) {
    return StreamPayment.builder()
      .accountId(accountId)
      .sourceAddress(InterledgerAddress.of("test.foo.bar"))
      .destinationAddress(InterledgerAddress.of("test.foo").with(streamPaymentId))
      .packetCount(1)
      .streamPaymentId(streamPaymentId)
      .amount(BigInteger.valueOf(amount))
      .assetScale((short) 9)
      .assetCode("XRP")
      .status(StreamPaymentStatus.PENDING)
      .type(StreamPaymentType.PAYMENT_RECEIVED)
      .createdAt(Instant.now())
      .modifiedAt(Instant.now())
      .build();
  }

  private String generateUuid() {
    return UUID.randomUUID().toString();
  }

}
