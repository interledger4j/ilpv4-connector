package org.interledger.connector.transactions;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerAddress;

import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class InMemoryPaymentTransactionManagerTest {

  public static final PageRequest DEFAULT_PAGE = PageRequest.of(0, 100);

  private InMemoryPaymentTransactionManager paymentTransactionManager;

  @Before
  public void setUp() {
    paymentTransactionManager = new InMemoryPaymentTransactionManager();
  }

  @Test
  public void findByAccountIdPagination() {
    AccountId accountId = AccountId.of(generateUuid());
    int transactionCount = 125;
    for (int i = 0; i < transactionCount; i++) {
      paymentTransactionManager.merge(newTransaction(accountId, generateUuid(), 10));
    }

    List<Transaction> trx1to50 = paymentTransactionManager.findByAccountId(
      accountId, PageRequest.of(0, 50)
    );

    assertThat(trx1to50).hasSize(50);

    List<Transaction> trx51To100 = paymentTransactionManager.findByAccountId(
      accountId, PageRequest.of(1, 50)
    );

    assertThat(trx51To100).hasSize(50);

    List<Transaction> trx101To125 = paymentTransactionManager.findByAccountId(
      accountId, PageRequest.of(2, 50)
    );

    assertThat(trx101To125).hasSize(25);


    assertThat(ImmutableSet.builder().addAll(trx1to50).addAll(trx51To100).addAll(trx101To125).build())
      .hasSize(transactionCount);
  }

  @Test
  public void merge() {
    AccountId accountId = AccountId.of(generateUuid());
    String transactionId = generateUuid();
    String transactionId2 = generateUuid();

    final Transaction entity1 = newTransaction(accountId, transactionId, 10);

    paymentTransactionManager.merge(entity1);

    final Transaction loadedAccessTokenEntity =
      paymentTransactionManager.findByAccountIdAndReferenceId(accountId, transactionId).get();

    assertEqual(loadedAccessTokenEntity, entity1);

    final Transaction entity2 = newTransaction(accountId, transactionId, 20);
    paymentTransactionManager.merge(entity2);

    assertThat(paymentTransactionManager.findByAccountId(accountId, DEFAULT_PAGE)).hasSize(1);
    Optional<Transaction> transaction1 =
      paymentTransactionManager.findByAccountIdAndReferenceId(accountId, transactionId);

    assertThat(transaction1).isPresent();
    assertThat(transaction1.get().packetCount()).isEqualTo(2);
    assertThat(transaction1.get().amount()).isEqualTo(UnsignedLong.valueOf(30));

    paymentTransactionManager.merge(newTransaction(accountId, transactionId2, 33));

    assertThat(paymentTransactionManager.findByAccountId(accountId, DEFAULT_PAGE)).hasSize(2);

    Optional<Transaction> transaction1Again =
      paymentTransactionManager.findByAccountIdAndReferenceId(accountId, transactionId);
    Optional<Transaction> transaction2 =
      paymentTransactionManager.findByAccountIdAndReferenceId(accountId, transactionId2);

    assertEqual(transaction1Again.get(), transaction1.get());

    assertThat(transaction2).isPresent();
    assertThat(transaction2.get().amount()).isEqualTo(UnsignedLong.valueOf(33));
    assertThat(transaction2.get().packetCount()).isEqualTo(1);
  }

  private void assertEqual(Transaction loadedAccessTokenEntity, Transaction entity1) {
    assertThat(loadedAccessTokenEntity).isEqualTo(entity1);
  }

  private Transaction newTransaction(AccountId accountId, String referenceId, long amount) {
    return Transaction.builder()
      .accountId(accountId)
      .sourceAddress(InterledgerAddress.of("test.foo.bar"))
      .destinationAddress(InterledgerAddress.of("test.foo").with(referenceId))
      .packetCount(1)
      .referenceId(referenceId)
      .amount(UnsignedLong.valueOf(amount))
      .assetScale((short) 9)
      .assetCode("XRP")
      .status(TransactionStatus.PENDING)
      .type(TransactionType.PAYMENT_RECEIVED)
      .createdAt(Instant.now())
      .modifiedAt(Instant.now())
      .build();
  }

  private String generateUuid() {
    return UUID.randomUUID().toString();
  }

}