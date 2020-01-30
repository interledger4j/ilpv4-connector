package org.interledger.connector.pubsub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

@Service
public class CoordinationMessageSubscriber implements MessageListener {

  public static List<String> messageList = new ArrayList<String>();

  private final ObjectMapper objectMapper;

  private final EventBus eventBus;

  private final CoordinationDedupeCache coordinationDedupeCache;

  private static final Logger LOGGER = LoggerFactory.getLogger(CoordinationMessageSubscriber.class);

  public CoordinationMessageSubscriber(ObjectMapper objectMapper, EventBus eventBus, CoordinationDedupeCache coordinationDedupeCache) {
    this.objectMapper = objectMapper;
    this.eventBus = eventBus;
    this.coordinationDedupeCache = coordinationDedupeCache;
  }

  public void onMessage(Message message, byte[] pattern) {
    try {
      CoordinationMessage received = objectMapper.readValue(message.getBody(), CoordinationMessage.class);

      if (coordinationDedupeCache.duplicate(received.messageUuid())) {
        LOGGER.info("Skipping message that was generated locally and received via coordination: {}", received);
      }

      final Object receivedBody = objectMapper.readValue(received.contents(), received.messageType());
      System.out.println(receivedBody);

      Class<?>[] interfaces = receivedBody.getClass().getInterfaces();

      if (interfaces.length == 0) {
        LOGGER.warn("Received a message that has no interfaces, meaning it cannot be proxies as Coordinated. " +
          "Rejecting coordination. {}", received);
        return;
      }

      Class<?>[] proxyInterfaces = new Class<?>[interfaces.length + 1];
      System.arraycopy(interfaces, 0, proxyInterfaces, 0, interfaces.length);
      proxyInterfaces[interfaces.length] = Coordinated.class;

      Object proxiedMessage = Proxy.newProxyInstance(
        getClass().getClassLoader(),
        proxyInterfaces,
        (proxy, method, methodArgs) -> method.invoke(receivedBody, methodArgs)
      );

      System.out.println("Proxied message: " + proxiedMessage);

      if (proxiedMessage instanceof Coordinated) {
        System.out.println("proxy detected as coordinated");
      }

      eventBus.post(proxiedMessage);
//      eventBus.post(receivedBody);

    } catch (Exception e) {
      LOGGER.error("Cannot proxy message", e);
    }
//    messageList.add(message.toString());
//    System.out.println("Message received: " + message.toString());
  }

}