package org.interledger.connector.pubsub;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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

  private UUID applicationUuid;

  private CoordinationMessageSubscriber subscriber;

  @Before
  public void setUp() {
    objectMapper = new ObjectMapper();
    applicationUuid = UUID.randomUUID();
    subscriber = new CoordinationMessageSubscriber(objectMapper, eventBus, applicationUuid);
  }

  @Test
  public void onMessage() throws Exception {
    SampleCoordinatedEvent event = SampleCoordinatedEvent.builder().build();

    CoordinationMessage message = buildMessage(event, UUID.randomUUID());

    byte[] body = objectMapper.writeValueAsBytes(message);

    Message redisMessage = mock(Message.class);
    when(redisMessage.getBody()).thenReturn(body);

    subscriber.onMessage(redisMessage, null);

    // mutating this will force an equals check against the event bus
    ((AbstractCoordinatedEvent) event).markReceivedViaCoordination();
    verify(eventBus, times(1)).post(event);
  }

  @Test
  public void onMessageIgnoresLocal() throws Exception {
    SampleCoordinatedEvent event = SampleCoordinatedEvent.builder().build();

    CoordinationMessage message = buildMessage(event, applicationUuid);

    byte[] body = objectMapper.writeValueAsBytes(message);

    Message redisMessage = mock(Message.class);
    when(redisMessage.getBody()).thenReturn(body);

    subscriber.onMessage(redisMessage, null);

    verifyNoInteractions(eventBus);
  }

  @Test
  public void onMessageFailsSilentlyWhenNotCoordinatedEvent() throws Exception {
    Integer event = 1;

    CoordinationMessage message = CoordinationMessage.builder()
      .messageClassName(event.getClass().getName())
      .contents(objectMapper.writeValueAsBytes(event))
      .applicationCoordinationUuid(UUID.randomUUID())
      .messageUuid(UUID.randomUUID())
      .build();

    byte[] body = objectMapper.writeValueAsBytes(message);

    Message redisMessage = mock(Message.class);
    when(redisMessage.getBody()).thenReturn(body);

    subscriber.onMessage(redisMessage, null);

    verifyNoInteractions(eventBus);
  }

  protected ImmutableCoordinationMessage buildMessage(SampleCoordinatedEvent event, UUID appUuid)
    throws JsonProcessingException {
    return CoordinationMessage.builder()
      .messageClassName(event.getClass().getName())
      .contents(objectMapper.writeValueAsBytes(event))
      .applicationCoordinationUuid(appUuid)
      .messageUuid(UUID.randomUUID())
      .build();
  }

}
