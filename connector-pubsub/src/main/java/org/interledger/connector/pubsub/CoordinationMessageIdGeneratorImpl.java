package org.interledger.connector.pubsub;

import java.util.UUID;

public class CoordinationMessageIdGeneratorImpl implements CoordinationMessageIdGenerator {

  @Override
  public UUID generatedMessageUUID() {
    return UUID.randomUUID();
  }
}
