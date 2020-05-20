package org.interledger.connector.payments;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.events.StreamPaymentClosedEvent;
import org.interledger.core.InterledgerAddress;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.primitives.UnsignedLong;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class DefaultIdlePendingPaymentsCloserTest {

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock
  private EventBus eventBus;

  @Mock
  private StreamPaymentManager streamPaymentsManager;

  @InjectMocks
  private DefaultIdlePendingPaymentsCloser paymentCloser;

  @Test
  public void closeExpiredPayments() {
    List<StreamPayment> pendingPayments = Lists.newArrayList(
      newPayment("payment1"),
      newPayment("payment2"),
      newPayment("payment3")
    );

    when(streamPaymentsManager.closeIdlePendingPayments(any(Instant.class))).thenReturn(pendingPayments);

    paymentCloser.closeExpiredPayments();

    pendingPayments.forEach(payment -> {
      verify(eventBus).post(
        StreamPaymentClosedEvent.builder().streamPayment(payment).build()
      );
    });
    verifyNoMoreInteractions(eventBus);
  }


  @Test
  public void closeExpiredPaymentsUpdatesStatusOfFailedToError() {
    StreamPayment goodPayment = newPayment("goodPayment");
    StreamPayment badPayment = newPayment("badPayment");
    List<StreamPayment> pendingPayments = Lists.newArrayList(
      goodPayment,
      badPayment
    );

    doThrow(new RuntimeException("fail"))
      .when(eventBus)
      .post(StreamPaymentClosedEvent.builder().streamPayment(badPayment).build());

    when(streamPaymentsManager.closeIdlePendingPayments(any(Instant.class))).thenReturn(pendingPayments);

    paymentCloser.closeExpiredPayments();

    pendingPayments.forEach(payment -> {
      verify(eventBus).post(
        StreamPaymentClosedEvent.builder().streamPayment(payment).build()
      );
    });

    verify(streamPaymentsManager).closeIdlePendingPayments(any(Instant.class));
    verify(streamPaymentsManager).updateStatus(badPayment.accountId(),
      badPayment.streamPaymentId(),
      StreamPaymentStatus.ERROR);

    verifyNoMoreInteractions(eventBus, streamPaymentsManager);
  }


  private StreamPayment newPayment(String id) {
    return StreamPayment.builder()
      .createdAt(Instant.now())
      .modifiedAt(Instant.now())
      .sourceAddress(InterledgerAddress.of("test.foo.bar"))
      .accountId(AccountId.of(UUID.randomUUID().toString()))
      .packetCount(1)
      .streamPaymentId("streamPayment")
      .amount(BigInteger.TEN)
      .assetCode("XRP")
      .assetScale((short) 9)
      .destinationAddress(InterledgerAddress.of("test.baz"))
      .status(StreamPaymentStatus.PENDING)
      .type(StreamPaymentType.PAYMENT_RECEIVED)
      .deliveredAmount(UnsignedLong.valueOf(10))
      .build();
  }
}