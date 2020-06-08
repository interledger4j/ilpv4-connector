package org.interledger.connector.jackson.modules;

import org.interledger.openpayments.ChargeId;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;


/**
 * A Jackson {@link SimpleModule} for serializing and deserializing instances of {@link ChargeId}.
 */
public class ChargeIdModule extends SimpleModule {

  private static final String NAME = "ChargeIdModule";

  /**
   * No-args Constructor.
   */
  public ChargeIdModule() {
    super(
      NAME,
      new Version(1, 0, 0, null, "org.interledger.connector",
        "charge-id")
    );

    addSerializer(ChargeId.class, new ChargeIdSerializer());
    addDeserializer(ChargeId.class, new ChargeIdDeserializer());
  }
}