package org.interledger.connector.jackson.modules;

import org.interledger.connector.accounts.AccountId;
import org.interledger.spsp.PaymentPointer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

import java.io.IOException;

/**
 * Jackson serializer {@link AccountId}.
 */
public class PaymentPointerSerializer extends StdScalarSerializer<PaymentPointer> {

  public PaymentPointerSerializer() {
    super(AccountId.class, false);
  }

  @Override
  public void serialize(PaymentPointer paymentPointer, JsonGenerator gen, SerializerProvider provider) throws IOException {
    gen.writeString(paymentPointer.toString());
  }
}
