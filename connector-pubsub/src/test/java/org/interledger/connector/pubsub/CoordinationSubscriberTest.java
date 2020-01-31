package org.interledger.connector.pubsub;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.event.AccountUpdatedEvent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.data.redis.connection.Message;

import java.util.UUID;

public class CoordinationSubscriberTest {

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  private ObjectMapper objectMapper;

  @Mock
  private EventBus eventBus;

  @Mock
  private CoordinationProxyGenerator proxyGenerator;

  private UUID applicationUuid;

  private CoordinationMessageSubscriber subscriber;

  @Before
  public void setUp() {
    objectMapper = new ObjectMapper();
    applicationUuid = UUID.randomUUID();
    subscriber = new CoordinationMessageSubscriber(objectMapper, eventBus, applicationUuid, proxyGenerator);
  }

  @Test
  public void onMessage() throws Exception {
    AccountUpdatedEvent update = buildUpdate();

    CoordinationMessage message = buildMessage(update, UUID.randomUUID());

    byte[] body = objectMapper.writeValueAsBytes(message);

    Message redisMessage = mock(Message.class);
    when(redisMessage.getBody()).thenReturn(body);
    when(proxyGenerator.createCoordinatedProxy(update)).thenReturn(update);

    subscriber.onMessage(redisMessage, null);

    verify(proxyGenerator, times(1)).createCoordinatedProxy(update);
    verify(eventBus, times(1)).post(update);
  }

  @Test
  public void onMessageIgnoresLocal() throws Exception {
    AccountUpdatedEvent update = buildUpdate();

    CoordinationMessage message = buildMessage(update, applicationUuid);

    byte[] body = objectMapper.writeValueAsBytes(message);

    Message redisMessage = mock(Message.class);
    when(redisMessage.getBody()).thenReturn(body);

    subscriber.onMessage(redisMessage, null);

    verifyNoInteractions(proxyGenerator);
    verifyNoInteractions(eventBus);
  }

  protected ImmutableCoordinationMessage buildMessage(AccountUpdatedEvent update, UUID appUuid)
    throws JsonProcessingException {
    return CoordinationMessage.builder()
      .messageClassName(update.getClass().getName())
      .contents(objectMapper.writeValueAsBytes(update))
      .applicationCoordinationUuid(appUuid)
      .messageUuid(UUID.randomUUID())
      .build();
  }

  protected AccountUpdatedEvent buildUpdate() {
    return AccountUpdatedEvent.builder()
        .accountId(AccountId.of("lisa"))
        .build();
  }

}
