package org.interledger.connector.jackson.modules;

import org.interledger.connector.accounts.AccountId;
import org.interledger.spsp.PaymentPointer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

/**
 * Jackson deserializer for {@link PaymentPointer}.
 */
public class PaymentPointerDeserializer extends StdDeserializer<PaymentPointer> {

  protected PaymentPointerDeserializer() {
    super(AccountId.class);
  }

  @Override
  public PaymentPointer deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
    return PaymentPointer.of(jsonParser.getText());
  }
}
