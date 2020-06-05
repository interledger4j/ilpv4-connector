package org.interledger.connector.jackson.modules;

import org.interledger.openpayments.CorrelationId;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

/**
 * Jackson deserializer for {@link CorrelationId}.
 */
public class CorrelationIdDeserializer extends StdDeserializer<CorrelationId> {

  protected CorrelationIdDeserializer() {
    super(CorrelationId.class);
  }

  @Override
  public CorrelationId deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
    return CorrelationId.of(jsonParser.getText());
  }
}
