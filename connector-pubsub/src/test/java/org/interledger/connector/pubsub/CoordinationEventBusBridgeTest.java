package org.interledger.connector.pubsub;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.event.AccountCreatedEvent;
import org.interledger.connector.accounts.event.AccountUpdatedEvent;

import com.google.common.eventbus.EventBus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class CoordinationEventBusBridgeTest {

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock
  private CoordinationMessagePublisher publisher;

  @Mock
  private EventBus eventBus;

  private CoordinationEventBusBridge bridge;

  @Before
  public void setup() {
    bridge = new CoordinationEventBusBridge(publisher, eventBus);
  }

  @Test
  public void constructorSubscribesItself() {
    verify(eventBus, times(1)).register(bridge);
  }

  @Test
  public void ignoresPreviouslyCoordinatedMessage() {
    AccountUpdatedEvent update = AccountUpdatedEvent.builder().accountId(AccountId.of("mark")).build();
    bridge.onAccountUpdated((AccountUpdatedEvent) new CoordinationProxyGeneratorImpl().createCoordinatedProxy(update));
    verifyNoInteractions(publisher);
  }

  @Test
  public void accountUpdate() {
    AccountUpdatedEvent update = AccountUpdatedEvent.builder().accountId(AccountId.of("mark")).build();
    bridge.onAccountUpdated(update);
    verify(publisher, times(1)).publish(update);
  }

  @Test
  public void accountCreate() {
    AccountCreatedEvent update = AccountCreatedEvent.builder().accountId(AccountId.of("mark")).build();
    bridge.onAccountCreated(update);
    verify(publisher, times(1)).publish(update);
  }

}
