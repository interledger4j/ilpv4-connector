package org.interledger.connector.pubsub;

import org.interledger.connector.accounts.event.AccountCreatedEvent;
import org.interledger.connector.accounts.event.AccountUpdatedEvent;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CoordinationEventBusBridge {

  private final CoordinationMessagePublisher coordinationMessagePublisher;

  private static final Logger LOGGER = LoggerFactory.getLogger(CoordinationEventBusBridge.class);

  public CoordinationEventBusBridge(CoordinationMessagePublisher coordinationMessagePublisher, EventBus eventBus) {
    this.coordinationMessagePublisher = coordinationMessagePublisher;
    eventBus.register(this);
    LOGGER.info("Bootstrapping {}; associated messages will propagate across cluster to subscribers", getClass().getName());
  }

  private void publish(Object event) {
    if (event instanceof Coordinated) {
      LOGGER.info("Message detected as previously coordinated and will not be reprocessed: {}", event);
    }
    else {
      coordinationMessagePublisher.publish(event);
    }
  }

  @Subscribe
  public void onAccountCreated(AccountCreatedEvent event) {
    publish(event);
  }

  @Subscribe
  public void onAccountUpdated(AccountUpdatedEvent event) {
    publish(event);
  }

//  @Subscribe
//  public void onStaticRouteCreated() {
//
//  }

}
