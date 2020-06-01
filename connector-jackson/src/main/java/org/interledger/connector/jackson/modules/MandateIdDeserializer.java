package org.interledger.connector.jackson.modules;

import org.interledger.connector.opa.model.MandateId;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

/**
 * Jackson deserializer for {@link MandateId}.
 */
public class MandateIdDeserializer extends StdDeserializer<MandateId> {

  protected MandateIdDeserializer() {
    super(MandateId.class);
  }

  @Override
  public MandateId deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
    return MandateId.of(jsonParser.getText());
  }
}