package org.interledger.connector.jackson.modules;

import org.interledger.openpayments.PaymentId;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

/**
 * Jackson deserializer for {@link PaymentId}.
 */
public class PaymentIdDeserializer extends StdDeserializer<PaymentId> {

  protected PaymentIdDeserializer() {
    super(PaymentId.class);
  }

  @Override
  public PaymentId deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
    return PaymentId.of(jsonParser.getText());
  }
}
