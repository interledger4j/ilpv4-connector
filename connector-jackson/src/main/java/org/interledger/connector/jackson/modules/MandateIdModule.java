package org.interledger.connector.jackson.modules;

import org.interledger.connector.opa.model.MandateId;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;


/**
 * A Jackson {@link SimpleModule} for serializing and deserializing instances of {@link MandateId}.
 */
public class MandateIdModule extends SimpleModule {

  private static final String NAME = "MandateIdModule";

  /**
   * No-args Constructor.
   */
  public MandateIdModule() {
    super(
      NAME,
      new Version(1, 0, 0, null, "org.interledger.connector",
        "mandate-id")
    );

    addSerializer(MandateId.class, new MandateIdSerializer());
    addDeserializer(MandateId.class, new MandateIdDeserializer());
  }
}