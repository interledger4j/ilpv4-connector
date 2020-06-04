package org.interledger.connector.jackson.modules;

import org.interledger.openpayments.CorrelationId;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;


/**
 * A Jackson {@link SimpleModule} for serializing and deserializing instances of {@link CorrelationId}.
 */
public class CorrelationIdModule extends SimpleModule {

  private static final String NAME = "CorrelationIdModule";

  /**
   * No-args Constructor.
   */
  public CorrelationIdModule() {
    super(
      NAME,
      new Version(1, 0, 0, null, "org.interledger.connector",
        "correlation-id")
    );

    addSerializer(CorrelationId.class, new CorrelationIdSerializer());
    addDeserializer(CorrelationId.class, new CorrelationIdDeserializer());
  }
}
