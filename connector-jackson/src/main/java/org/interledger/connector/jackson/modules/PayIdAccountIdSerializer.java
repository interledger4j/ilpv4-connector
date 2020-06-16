package org.interledger.connector.jackson.modules;

import org.interledger.openpayments.PayIdAccountId;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

import java.io.IOException;

/**
 * Jackson serializer {@link PayIdAccountId}.
 */
public class PayIdAccountIdSerializer extends StdScalarSerializer<PayIdAccountId> {

  public PayIdAccountIdSerializer() {
    super(PayIdAccountId.class, false);
  }

  @Override
  public void serialize(PayIdAccountId mandateId, JsonGenerator gen, SerializerProvider provider) throws IOException {
    gen.writeString(mandateId.value().toString());
  }
}