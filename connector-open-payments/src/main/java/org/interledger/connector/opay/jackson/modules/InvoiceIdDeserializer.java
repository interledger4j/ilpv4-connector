package org.interledger.connector.opay.jackson.modules;

import org.interledger.connector.opay.InvoiceId;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.UUID;

/**
 * Jackson deserializer for {@link InvoiceId}.
 */
public class InvoiceIdDeserializer extends StdDeserializer<InvoiceId> {

  protected InvoiceIdDeserializer() {
    super(InvoiceId.class);
  }

  @Override
  public InvoiceId deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
    return InvoiceId.of(UUID.fromString(jsonParser.getText()));
  }
}
