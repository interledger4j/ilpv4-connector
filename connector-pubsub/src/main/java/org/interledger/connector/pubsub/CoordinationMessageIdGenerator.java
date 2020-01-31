package org.interledger.connector.pubsub;

import java.util.UUID;

public interface CoordinationMessageIdGenerator {

  UUID generatedMessageUUID();
}
