package org.interledger.connector.jackson.modules;


import org.interledger.openpayments.PaymentId;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

import java.io.IOException;

/**
 * Jackson serializer {@link PaymentId}.
 */
public class PaymentIdSerializer extends StdScalarSerializer<PaymentId> {

  public PaymentIdSerializer() {
    super(PaymentId.class, false);
  }

  @Override
  public void serialize(PaymentId paymentId, JsonGenerator gen, SerializerProvider provider) throws IOException {
    gen.writeString(paymentId.value());
  }
}
