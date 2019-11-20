package org.interledger.connector.it.pubsub;

import com.google.cloud.pubsub.v1.Publisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gcp.autoconfigure.pubsub.GcpPubSubProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

import java.io.IOException;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Creates the topic and subscription in GCP emulator. If emulator is not running, this will hang.
 */
@Configuration
public class TestPubSubConfig {

    private final String topicName;
    private final String subscriptionName;
    private final PubSubResourceGenerator pubSubResourceGenerator;

    TestPubSubConfig(PubSubResourceGenerator pubSubResourceGenerator,
                     @Value("${interledger.connector.pubsub.topics.fulfillment-event}") String fulfillmentEventTopic) {
        this.pubSubResourceGenerator = pubSubResourceGenerator;
        this.topicName = fulfillmentEventTopic;
        this.subscriptionName = fulfillmentEventTopic + ".subscription";
    }

    @PostConstruct
    void createResources() {
        pubSubResourceGenerator.createTopic(topicName);
        pubSubResourceGenerator.createSubscription(topicName, subscriptionName);
    }

    @Bean
    public Publisher testPublisher() throws IOException {
        return pubSubResourceGenerator.createPublisher(topicName);
    }

}
