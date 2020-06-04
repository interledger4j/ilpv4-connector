package org.interledger.connector.jackson.modules;


import org.interledger.openpayments.PaymentId;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;


/**
 * A Jackson {@link SimpleModule} for serializing and deserializing instances of {@link PaymentId}.
 */
public class PaymentIdModule extends SimpleModule {

  private static final String NAME = "PaymentIdModule";

  /**
   * No-args Constructor.
   */
  public PaymentIdModule() {
    super(
      NAME,
      new Version(1, 0, 0, null, "org.interledger.connector",
        "payment-id")
    );

    addSerializer(PaymentId.class, new PaymentIdSerializer());
    addDeserializer(PaymentId.class, new PaymentIdDeserializer());
  }
}
