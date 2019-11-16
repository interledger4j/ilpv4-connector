package org.interledger.connector.server.spring.gcp;

import org.interledger.connector.events.PacketFulfillmentEvent;
import org.interledger.connector.gcp.DefaultFulfillmentPublisher;
import org.interledger.connector.gcp.FulfillmentPublisher;
import org.interledger.connector.metrics.MetricsService;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
@ConditionalOnProperty("spring.cloud.gcp.project-id")
public class GcpPubSubConfig {

  @Autowired
  private FulfillmentPublisher fulfillmentPublisher;

  @Autowired
  private EventBus eventBus;

  @PostConstruct
  protected void register() {
    eventBus.register(this);
  }

  @Bean
  public FulfillmentPublisher fulfillmentPublisher() {
    return new DefaultFulfillmentPublisher();
  }

  @Subscribe
  public void handleFulfillment(PacketFulfillmentEvent event) {
    fulfillmentPublisher.publish(event);
  }


}
