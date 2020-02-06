package org.interledger.connector.pubsub;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class CoordinationMessageIdGeneratorTest {

  private final CoordinationMessageIdGenerator generator = new CoordinationMessageIdGeneratorImpl();

  @Test
  public void itsJustNotTheSame() {
    assertThat(generator.generatedMessageUUID()).isNotEqualTo(generator.generatedMessageUUID());
  }
}
