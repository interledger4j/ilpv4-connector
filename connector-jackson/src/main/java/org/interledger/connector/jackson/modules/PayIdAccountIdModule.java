package org.interledger.connector.jackson.modules;

import org.interledger.openpayments.PayIdAccountId;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;


/**
 * A Jackson {@link SimpleModule} for serializing and deserializing instances of {@link PayIdAccountId}.
 */
public class PayIdAccountIdModule extends SimpleModule {

  private static final String NAME = "PayIdAccountIdModule";

  /**
   * No-args Constructor.
   */
  public PayIdAccountIdModule() {
    super(
      NAME,
      new Version(1, 0, 0, null, "org.interledger.connector",
        "mandate-id")
    );

    addSerializer(PayIdAccountId.class, new PayIdAccountIdSerializer());
    addDeserializer(PayIdAccountId.class, new PayIdAccountIdDeserializer());
  }
}