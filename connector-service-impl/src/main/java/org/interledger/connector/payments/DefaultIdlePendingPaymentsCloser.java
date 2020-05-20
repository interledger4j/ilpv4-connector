package org.interledger.connector.payments;

import static org.slf4j.LoggerFactory.getLogger;

import org.interledger.connector.events.StreamPaymentClosedEvent;

import com.google.common.eventbus.EventBus;
import org.slf4j.Logger;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;


public class DefaultIdlePendingPaymentsCloser implements IdlePendingPaymentsCloser {

  private static final Logger LOGGER = getLogger(DefaultIdlePendingPaymentsCloser.class);

  public static final int EVERY_THIRTY_SECONDS = 30000;
  private final EventBus eventBus;

  private final StreamPaymentManager streamPaymentManager;

  public DefaultIdlePendingPaymentsCloser(EventBus eventBus,
                                          StreamPaymentManager streamPaymentManager) {
    this.eventBus = eventBus;
    this.streamPaymentManager = streamPaymentManager;
  }

  /**
   * Closes any payments in PENDING status that have not had any activity in the last 60s. Payments are updated to
   * CLOSED_EXPIRED status and then an {@link StreamPaymentClosedEvent} is emitted. If event cannot be emitted,
   * then payment status is updated to ERROR. The entire method is done in an isolated transaction so that if the
   * server dies in the middle of processing, no status updates are committed. Because of this, it is possible that a
   * duplicate event could be emitted in the case that a server dies while this method is executing because the
   * rolled back PENDING payments will get picked up again after the server restarts.
   */
  @Override
  @Async
  @Scheduled(fixedRate = EVERY_THIRTY_SECONDS)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void closeExpiredPayments() {
    List<StreamPayment> updated = streamPaymentManager.closeIdlePendingPayments(Instant.now().minusSeconds(60));
    updated.forEach(payment -> {
      try {
        eventBus.post(StreamPaymentClosedEvent.builder().streamPayment(payment).build());
      } catch (Exception e) {
        streamPaymentManager.updateStatus(payment.accountId(), payment.streamPaymentId(), StreamPaymentStatus.ERROR);
        LOGGER.error("failed post close expired payments", e);
      }
    });
  }

}
