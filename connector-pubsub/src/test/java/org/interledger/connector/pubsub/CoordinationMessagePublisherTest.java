package org.interledger.connector.pubsub;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;

import java.util.UUID;

public class CoordinationMessagePublisherTest {

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private RedisTemplate<String, ?> template;

  @Mock
  private ChannelTopic topic;

  @Mock
  private ObjectMapper objectMapper;

  @Mock
  private CoordinationMessageIdGenerator idGenerator;

  private UUID applicationUuid;

  private CoordinationMessagePublisher publisher;

  @Before
  public void setUp() {
    applicationUuid = UUID.randomUUID();
    publisher = new CoordinationMessagePublisherImpl(template, topic, objectMapper, applicationUuid, idGenerator);
  }

  @Test
  public void nullMakesPublishSad() {
    expectedException.expect(NullPointerException.class);
    publisher.publish(null);
  }

  @Test
  public void coordinatedMessageMakesPublishSad() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Cannot republish a message received via coordination already");
    ImmutableSampleCoordinatedEvent event = ImmutableSampleCoordinatedEvent.builder().build();
    event.markReceivedViaCoordination();
    publisher.publish(event);
  }

  @Test
  public void publish() throws Exception {
    ImmutableSampleCoordinatedEvent event = ImmutableSampleCoordinatedEvent.builder().build();
    byte[] contents = "what a story mark".getBytes();
    UUID messageId = UUID.randomUUID();
    CoordinationMessage coordinationMessage = CoordinationMessage.builder()
      .applicationCoordinationUuid(applicationUuid)
      .messageUuid(messageId)
      .contents(contents)
      .messageClassName(ImmutableSampleCoordinatedEvent.class.getName())
      .build();

    when(objectMapper.writeValueAsBytes(event)).thenReturn(contents);
    when(objectMapper.writeValueAsString(coordinationMessage)).thenReturn("you're tearing me apart, lisa!");
    when(idGenerator.generatedMessageUUID()).thenReturn(messageId);
    when(topic.getTopic()).thenReturn("the room");

    publisher.publish(event);
    verify(template, times(1)).convertAndSend("the room", "you're tearing me apart, lisa!");
    verify(objectMapper, times(1)).writeValueAsBytes(event);
    verify(objectMapper, times(1)).writeValueAsString(coordinationMessage);
  }
}
