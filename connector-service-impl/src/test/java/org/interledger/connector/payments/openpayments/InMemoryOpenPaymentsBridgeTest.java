package org.interledger.connector.payments.openpayments;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.opa.model.ImmutableInvoice;
import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.InvoiceId;
import org.interledger.connector.opa.model.PaymentNetwork;
import org.interledger.connector.payments.ImmutableStreamPayment.Builder;
import org.interledger.connector.paymen.OpenPaymentsEventHandler;
import org.interledger.connector.payments.StreamPayment;
import org.interledger.connector.payments.StreamPaymentStatus;
import org.interledger.connector.payments.StreamPaymentType;
import org.interledger.core.InterledgerAddress;
import org.interledger.openpayments.events.PaymentCompletedEvent;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.primitives.UnsignedLong;
import org.interleger.openpayments.PaymentSystemEventHandler;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Unit test for validating the OpenPayments/Connector bridge.
 */
public class InMemoryOpenPaymentsBridgeTest {

  private static final int NUM_EVENT_BUS_THREADS = 10;
  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private InMemoryOpenPaymentsBridge openPaymentsBridge;

  private CountDownLatch numInvoiceCreatedInvocations;
  private CountDownLatch numPaymentCompletedInvocations;

  private static <T> List<T> awaitResults(final List<CompletableFuture<T>> futures) {
    Objects.requireNonNull(futures);
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    return futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
  }

  @Before
  public void setUp() {
    this.numInvoiceCreatedInvocations = new CountDownLatch(1);
    this.numPaymentCompletedInvocations = new CountDownLatch(1);

    final EventBus eventBus = new AsyncEventBus(
      Executors.newFixedThreadPool(NUM_EVENT_BUS_THREADS)
    );

    // The Connector's view of the OpenPayments Server
    OpenPaymentsEventHandler openPaymentsEventHandler = new OpenPaymentsEventHandler() {

      @Override
      public void onInvoiceCreated(final org.interledger.connector.events.InvoiceCreateEvent invoiceCreateEvent) {
        numInvoiceCreatedInvocations.countDown();
      }
    };

    // The OpenPayments Server's view of the Connector
    PaymentSystemEventHandler paymentSystemEventHandler = new PaymentSystemEventHandler() {

      @Override
      public void onPaymentCompleted(final PaymentCompletedEvent paymentCompletedEvent) {
        numPaymentCompletedInvocations.countDown();
      }
    };

    // InMemoryOpenPaymentsBridge is both a PaymentsSystemFacade AND a OpenPaymentsFacade
    this.openPaymentsBridge = new InMemoryOpenPaymentsBridge(openPaymentsEventHandler, paymentSystemEventHandler,
      paymentSystemFacadeDelegate);
  }

  /**
   * Simulates an invoice created in a faux OpenPayments server and validates that InMemoryOpenPaymentsBridge properly
   * bridges the call.
   */
  @Test
  public void testInvoiceCreated() throws InterruptedException {
    // An OpenPayments Server will have a PaymentSystemFacade.
    this.openPaymentsBridge.emitInvoiceCreated(constructInvoice().build());

    this.numInvoiceCreatedInvocations.await(5, TimeUnit.SECONDS);
    assertThat(numInvoiceCreatedInvocations.getCount()).isEqualTo(0);
    assertThat(numPaymentCompletedInvocations.getCount()).isEqualTo(1);
  }

  @Test
  public void testPaymentCreated() throws InterruptedException {
    // An OpenPayments Server will have a PaymentSystemFacade.
    this.openPaymentsBridge.publishStreamPaymentCompleted(this.constructStreamPaymentBuilder().build());

    this.numPaymentCompletedInvocations.await(5, TimeUnit.SECONDS);
    assertThat(numInvoiceCreatedInvocations.getCount()).isEqualTo(1);
    assertThat(numPaymentCompletedInvocations.getCount()).isEqualTo(0);
  }

  @Test
  public void testTogether() throws InterruptedException {
    // An OpenPayments Server will have a PaymentSystemFacade.
    this.openPaymentsBridge.publishStreamPaymentCompleted(constructStreamPaymentBuilder().build());
    this.openPaymentsBridge.emitInvoiceCreated(constructInvoice().build());

    this.numInvoiceCreatedInvocations.await(5, TimeUnit.SECONDS);
    this.numPaymentCompletedInvocations.await(5, TimeUnit.SECONDS);
    assertThat(numInvoiceCreatedInvocations.getCount()).isEqualTo(0);
    assertThat(numPaymentCompletedInvocations.getCount()).isEqualTo(0);
  }

  @Test
  public void testMultiThreadedTogether() {
    final InvoiceId invoiceId = InvoiceId.of(UUID.randomUUID().toString());
    int runCount = 1000;

    this.numInvoiceCreatedInvocations = new CountDownLatch(runCount);
    this.numPaymentCompletedInvocations = new CountDownLatch(runCount);

    final StreamPayment streamPayment = this.constructStreamPaymentBuilder()
      .streamPaymentId(UUID.randomUUID().toString())
      .build();

    List<CompletableFuture<Object>> results =
      runInParallel(10, runCount, () -> {
        this.openPaymentsBridge.publishStreamPaymentCompleted(streamPayment);
        this.openPaymentsBridge.emitInvoiceCreated(this.constructInvoice().build());
      });

    awaitResults(results).forEach(result -> {
      // An OpenPayments Server will have a PaymentSystemFacade.
      assertThat(numInvoiceCreatedInvocations.getCount()).isEqualTo(0);
      assertThat(numPaymentCompletedInvocations.getCount()).isEqualTo(0);
    });
  }

  //////////////////
  // Private Helpers
  //////////////////

  private Builder constructStreamPaymentBuilder() {
    return StreamPayment.builder()
      .createdAt(Instant.now())
      .modifiedAt(Instant.now())
      .assetCode("USD")
      .assetScale((short) 8)
      .streamPaymentId("stream-payment-555")
      .type(StreamPaymentType.PAYMENT_RECEIVED)
      .amount(BigInteger.TEN)
      .status(StreamPaymentStatus.CLOSED_BY_STREAM)
      .packetCount(10)
      .accountId(AccountId.of("account-987"))
      .destinationAddress(InterledgerAddress.of("example.destination"))
      .sourceAddress(InterledgerAddress.of("example.source"));
  }

  private List<CompletableFuture<Object>> runInParallel(int numThreads, int runCount, Runnable task) {
    ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
    List<CompletableFuture<Object>> tasks = IntStream.range(0, runCount)
      .mapToObj((taskId) -> CompletableFuture.supplyAsync(() -> {
        logger.info("Starting task " + taskId);
        try {
          task.run();
          logger.info("Finished task " + taskId);
          return null;
        } catch (Exception e) {
          logger.warn("Failed task " + taskId, e);
          throw new RuntimeException(e);
        }
      }, executorService))
      .collect(Collectors.toList());
    executorService.shutdown();
    return tasks;
  }

  private ImmutableInvoice.Builder constructInvoice() {
    return Invoice.builder()
      .id(InvoiceId.of(UUID.randomUUID().toString()))
      .createdAt(Instant.now())
      .updatedAt(Instant.now())
      .accountId("account-" + UUID.randomUUID().toString())
      .paymentId("paymentid-123")
      .paymentNetwork(PaymentNetwork.ILP)
      .subject("$xpring.money/alice")
      .amount(UnsignedLong.valueOf(10L))
      .received(UnsignedLong.ZERO)
      .assetCode("USD")
      .assetScale((short) 8)
      .description("My cool shoes")
      .expiresAt(Instant.now().plus(20, ChronoUnit.MINUTES))
      .finalizedAt(Instant.now().plusSeconds(60));
  }
}
