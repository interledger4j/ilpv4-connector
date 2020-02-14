package org.interledger.connector.pubsub;

import java.util.UUID;

/**
 * Generates {@link UUID}s to uniquely identify messages.
 */
public interface CoordinationMessageIdGenerator {

  UUID generatedMessageUUID();
}
