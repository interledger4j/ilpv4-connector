package org.interledger.connector.server.spring.gcp;

import org.interledger.connector.events.PacketFullfillmentEvent;
import org.interledger.connector.events.PacketRejectionEvent;
import org.interledger.connector.gcp.DefaultGcpPacketResponseEventPublisher;
import org.interledger.connector.gcp.GcpPacketResponseEventPublisher;
import org.interledger.connector.settings.ConnectorSettings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.Optional;
import java.util.function.Supplier;
import javax.annotation.PostConstruct;

@Configuration
@ConditionalOnProperty("spring.cloud.gcp.pubsub.project-id")
public class GcpPubSubConfig {

  @Autowired
  private GcpPacketResponseEventPublisher eventPublisher;

  @Autowired
  private EventBus eventBus;

  @PostConstruct
  protected void register() {
    eventBus.register(this);
  }

  @Bean
  public GcpPacketResponseEventPublisher fulfillmentPublisher(
    PubSubTemplate template,
    @Value("${interledger.connector.pubsub.topics.fulfillment-event:#{null}}") Optional<String> fulfillmentEventTopicName,
    @Value("${interledger.connector.pubsub.topics.rejection-event:#{null}}") Optional<String> rejectionEventTopicName,
    ObjectMapper objectMapper,
    Supplier<ConnectorSettings> connectorSettingsSupplier,
    Clock clock) {
    return new DefaultGcpPacketResponseEventPublisher(template,
      fulfillmentEventTopicName,
      rejectionEventTopicName,
      connectorSettingsSupplier.get().operatorAddress(),
      objectMapper,
      clock);
  }

  @Subscribe
  public void handleFulfillment(PacketFullfillmentEvent event) {
    eventPublisher.publish(event);
  }

  @Subscribe
  public void handleRejection(PacketRejectionEvent event) {
    eventPublisher.publish(event);
  }


}
