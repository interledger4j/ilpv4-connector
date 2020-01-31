package org.interledger.connector.pubsub;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class AbstractCoordinatedEventTest {

  @Test
  public void mutateAndCheckEquals() {
    ImmutableSampleCoordinatedEvent e1 = SampleCoordinatedEvent.builder().build();
    ImmutableSampleCoordinatedEvent e2 = SampleCoordinatedEvent.builder().build();

    assertThat(e1).isInstanceOf(AbstractCoordinatedEvent.class);
    assertThat(e2).isInstanceOf(AbstractCoordinatedEvent.class);

    assertThat(e1).isEqualTo(e2);
    assertThat(e2).isEqualTo(e1);
    assertThat(e1.receivedViaCoordination()).isFalse();
    assertThat(e2.receivedViaCoordination()).isFalse();

    e1.markReceivedViaCoordination();
    assertThat(e1).isNotEqualTo(e2);
    assertThat(e2).isNotEqualTo(e1);
    assertThat(e1.receivedViaCoordination()).isTrue();
    assertThat(e2.receivedViaCoordination()).isFalse();

    e2.markReceivedViaCoordination();
    assertThat(e1).isEqualTo(e2);
    assertThat(e2).isEqualTo(e1);
    assertThat(e1.receivedViaCoordination()).isTrue();
    assertThat(e2.receivedViaCoordination()).isTrue();
  }

}
