package org.interledger.connector.opay.jackson.modules;

import org.interledger.spsp.PaymentPointer;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class PaymentPointerModule extends SimpleModule {
  private static final String NAME = "PaymentPointerModule";

  /**
   * No-args Constructor.
   */
  public PaymentPointerModule() {
    super(
      NAME,
      new Version(1, 0, 0, null, "org.interledger.connector",
        "payment-pointer-module")
    );

    addSerializer(PaymentPointer.class, new PaymentPointerSerializer());
    addDeserializer(PaymentPointer.class, new PaymentPointerDeserializer());
  }
}
