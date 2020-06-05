package org.interledger.connector.jackson.modules;

import org.interledger.openpayments.InvoiceId;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;


/**
 * A Jackson {@link SimpleModule} for serializing and deserializing instances of {@link InvoiceId}.
 */
public class InvoiceIdModule extends SimpleModule {

  private static final String NAME = "InvoiceIdModule";

  /**
   * No-args Constructor.
   */
  public InvoiceIdModule() {
    super(
      NAME,
      new Version(1, 0, 0, null, "org.interledger.connector",
        "invoice-id")
    );

    addSerializer(InvoiceId.class, new InvoiceIdSerializer());
    addDeserializer(InvoiceId.class, new InvoiceIdDeserializer());
  }
}
