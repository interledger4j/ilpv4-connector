package org.interledger.connector.jackson.modules;

import org.interledger.connector.opa.model.InvoiceId;
import org.interledger.connector.opa.model.PaymentId;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

/**
 * Jackson deserializer for {@link InvoiceId}.
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
