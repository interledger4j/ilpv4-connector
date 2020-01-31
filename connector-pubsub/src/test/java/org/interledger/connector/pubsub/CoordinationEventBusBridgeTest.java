package org.interledger.connector.pubsub;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.google.common.eventbus.EventBus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class CoordinationEventBusBridgeTest {

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private CoordinationMessagePublisher publisher;

  @Mock
  private EventBus eventBus;

  private CoordinationEventBusBridge bridge;

  @Before
  public void setUp() {
    bridge = new CoordinationEventBusBridge(publisher, eventBus);
  }

  @Test
  public void constructorSubscribesItself() {
    verify(eventBus, times(1)).register(bridge);
  }

  @Test
  public void ignoresPreviouslyCoordinatedMessage() {
    TestCoordinatedEvent event = new TestCoordinatedEvent();
    event.markReceivedViaCoordination();
    bridge.onCoordinatedEvent(event);
    verifyNoInteractions(publisher);
  }

  @Test
  public void event() {
    TestCoordinatedEvent event = new TestCoordinatedEvent();
    bridge.onCoordinatedEvent(event);
    verify(publisher, times(1)).publish(event);
  }

  @Test
  public void nullEventFails() {
    expectedException.expect(NullPointerException.class);
    bridge.onCoordinatedEvent(null);
  }

  private static class TestCoordinatedEvent extends AbstractCoordinatedEvent {}

}
