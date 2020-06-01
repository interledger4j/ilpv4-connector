package org.interledger.connector.jackson.modules;

import org.interledger.connector.opa.model.ChargeId;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

/**
 * Jackson deserializer for {@link ChargeId}.
 */
public class ChargeIdDeserializer extends StdDeserializer<ChargeId> {

  protected ChargeIdDeserializer() {
    super(ChargeId.class);
  }

  @Override
  public ChargeId deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
    return ChargeId.of(jsonParser.getText());
  }
}