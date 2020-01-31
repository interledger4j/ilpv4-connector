package org.interledger.connector.pubsub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service that subscribes to the shared topic for the cluster in order to forward messages potentially received
 * by another connector in the cluster.
 */
@Service
public class CoordinationMessageSubscriber implements MessageListener {

  private final ObjectMapper objectMapper;

  private final EventBus eventBus;

  private final UUID applicationCoordinationUuid;

  private static final Logger LOGGER = LoggerFactory.getLogger(CoordinationMessageSubscriber.class);

  public CoordinationMessageSubscriber(ObjectMapper objectMapper,
                                       EventBus eventBus,
                                       UUID applicationCoordinationUuid) {
    this.objectMapper = objectMapper;
    this.eventBus = eventBus;
    this.applicationCoordinationUuid = applicationCoordinationUuid;
  }

  /**
   * Processes messages that were sent via the shared topic and forwards them to the application in their original
   * form if they weren't originally sent by this connector instance. Proxies the original message for prevention
   * of feedback loops resulting from cluster messages being forwarded by connectors back and forth to one another.
   * @param message received via the shared topic
   * @param pattern not used
   */
  public void onMessage(Message message, byte[] pattern) {
    try {
      CoordinationMessage received = objectMapper.readValue(message.getBody(), CoordinationMessage.class);

      if (publishedByMe(received)) {
        LOGGER.debug("Skipping message that was generated locally and received via coordination: {}", received);
        return;
      }

      final Object receivedBody = objectMapper.readValue(received.contents(), Class.forName(received.messageClassName()));

      try {
        ((AbstractCoordinatedEvent) receivedBody).markReceivedViaCoordination();
      }
      catch (Exception e) {
        LOGGER.warn("Received a message that cannot be cast to AbstractCoordinatedEvent. Discarding. {}",
          receivedBody);
      }

      eventBus.post(receivedBody);

    } catch (Exception e) {
      LOGGER.error("Cannot proxy message", e);
    }
  }

  protected boolean publishedByMe(CoordinationMessage received) {
    return applicationCoordinationUuid.equals(received.applicationCoordinationUuid());
  }

}