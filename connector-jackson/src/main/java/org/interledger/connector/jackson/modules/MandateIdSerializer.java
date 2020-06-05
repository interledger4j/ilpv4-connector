package org.interledger.connector.jackson.modules;

import org.interledger.openpayments.MandateId;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

import java.io.IOException;

/**
 * Jackson serializer {@link MandateId}.
 */
public class MandateIdSerializer extends StdScalarSerializer<MandateId> {

  public MandateIdSerializer() {
    super(MandateId.class, false);
  }

  @Override
  public void serialize(MandateId mandateId, JsonGenerator gen, SerializerProvider provider) throws IOException {
    gen.writeString(mandateId.value().toString());
  }
}