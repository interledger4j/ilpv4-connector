package org.interledger.connector.wallet;

import org.interledger.connector.events.StreamPaymentClosedEvent;
import org.interledger.connector.payments.StreamPayment;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import java.util.Objects;

public class PaymentEventTranslator {

  private final EventBus eventBus;

  public PaymentEventTranslator(EventBus eventBus) {
    this.eventBus = eventBus;
  }


  /**
   * Handler that is called whenever a {@link StreamPaymentClosedEvent} is posted to the event bus.
   *
   * @param streamPaymentClosedEvent A {@link StreamPaymentClosedEvent}.
   */
  @Subscribe
  public void onStreamPaymentClosed(final StreamPaymentClosedEvent streamPaymentClosedEvent) {
    Objects.requireNonNull(streamPaymentClosedEvent);
    StreamPayment payment = streamPaymentClosedEvent.streamPayment();
    if (payment.correlationId().isPresent()) {
      onPayment(payment);
    }
  }
}
