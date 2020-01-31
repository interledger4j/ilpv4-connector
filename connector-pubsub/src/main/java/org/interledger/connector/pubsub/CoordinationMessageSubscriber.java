package org.interledger.connector.pubsub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CoordinationMessageSubscriber implements MessageListener {

  private final ObjectMapper objectMapper;

  private final EventBus eventBus;

  private final UUID applicationCoordinationUuid;

  private final CoordinationProxyGenerator coordinatedProxyGenerator;

  private static final Logger LOGGER = LoggerFactory.getLogger(CoordinationMessageSubscriber.class);

  public CoordinationMessageSubscriber(ObjectMapper objectMapper,
                                       EventBus eventBus,
                                       UUID applicationCoordinationUuid,
                                       CoordinationProxyGenerator coordinatedProxyGenerator) {
    this.objectMapper = objectMapper;
    this.eventBus = eventBus;
    this.applicationCoordinationUuid = applicationCoordinationUuid;
    this.coordinatedProxyGenerator = coordinatedProxyGenerator;
  }

  public void onMessage(Message message, byte[] pattern) {
    try {
      CoordinationMessage received = objectMapper.readValue(message.getBody(), CoordinationMessage.class);

      if (applicationCoordinationUuid.equals(received.applicationCoordinationUuid())) {
        // FIXME - reduce scope
        LOGGER.info("Skipping message that was generated locally and received via coordination: {}", received);
        return;
      }

      final Object receivedBody = objectMapper.readValue(received.contents(), Class.forName(received.messageClassName()));

      if (receivedBody.getClass().getInterfaces().length == 0) {
        LOGGER.warn("Received a message that has no interfaces, meaning it cannot be proxies as Coordinated. " +
          "Rejecting coordination. {}", received);
        return;
      }

      // FIXME - reduce scope
      LOGGER.info("Received cluster pubsub message: {}", receivedBody);

      Object proxiedMessage = coordinatedProxyGenerator.createCoordinatedProxy(receivedBody);

      eventBus.post(proxiedMessage);
//      eventBus.post(receivedBody);

    } catch (Exception e) {
      LOGGER.error("Cannot proxy message", e);
    }
  }

}