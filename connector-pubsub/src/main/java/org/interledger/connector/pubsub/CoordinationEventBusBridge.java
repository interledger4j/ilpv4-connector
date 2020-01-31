package org.interledger.connector.pubsub;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Service that registers itself to receive messages published to the connector's global {@link EventBus} that can
 * be forwarded to a shared topic that all connectors in the cluster listen to.
 */
@Service
public class CoordinationEventBusBridge {

  private final CoordinationMessagePublisher coordinationMessagePublisher;

  private static final Logger LOGGER = LoggerFactory.getLogger(CoordinationEventBusBridge.class);

  public CoordinationEventBusBridge(CoordinationMessagePublisher coordinationMessagePublisher, EventBus eventBus) {
    this.coordinationMessagePublisher = coordinationMessagePublisher;
    eventBus.register(this);
    LOGGER.info("Bootstrapping {}; associated messages will propagate across cluster to subscribers", getClass().getName());
  }

  /**
   * Inspects event to make sure it's not null and hasn't previously come through via the shared topic.
   * @param event potentially forwardable event
   */
  private void publish(AbstractCoordinatedEvent event) {
    Objects.requireNonNull(event);
    if (receivedViaCoordination(event)) {
      LOGGER.info("Message detected as previously coordinated and will not be reprocessed: {}", event);
    }
    else {
      coordinationMessagePublisher.publish(event);
    }
  }

  private boolean receivedViaCoordination(AbstractCoordinatedEvent event) {
    return event.receivedViaCoordination();
  }

  @Subscribe
  public void onCoordinatedEvent(AbstractCoordinatedEvent event) {
    publish(event);
  }

}
