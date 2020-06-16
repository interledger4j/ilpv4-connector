package org.interledger.connector.jackson.modules;

import org.interledger.openpayments.PayIdAccountId;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

/**
 * Jackson deserializer for {@link PayIdAccountId}.
 */
public class PayIdAccountIdDeserializer extends StdDeserializer<PayIdAccountId> {

  protected PayIdAccountIdDeserializer() {
    super(PayIdAccountId.class);
  }

  @Override
  public PayIdAccountId deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
    return PayIdAccountId.of(jsonParser.getText());
  }
}