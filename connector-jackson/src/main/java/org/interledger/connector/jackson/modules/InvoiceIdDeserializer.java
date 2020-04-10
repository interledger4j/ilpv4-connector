package org.interledger.connector.jackson.modules;

import org.interledger.connector.opa.model.InvoiceId;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

/**
 * Jackson deserializer for {@link InvoiceId}.
 */
public class InvoiceIdDeserializer extends StdDeserializer<InvoiceId> {

  protected InvoiceIdDeserializer() {
    super(InvoiceId.class);
  }

  @Override
  public InvoiceId deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
    return InvoiceId.of(jsonParser.getText());
  }
}