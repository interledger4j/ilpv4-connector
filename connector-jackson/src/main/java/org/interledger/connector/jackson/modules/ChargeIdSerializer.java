package org.interledger.connector.jackson.modules;

import org.interledger.connector.opa.model.ChargeId;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

import java.io.IOException;

/**
 * Jackson serializer {@link ChargeId}.
 */
public class ChargeIdSerializer extends StdScalarSerializer<ChargeId> {

  public ChargeIdSerializer() {
    super(ChargeId.class, false);
  }

  @Override
  public void serialize(ChargeId chargeId, JsonGenerator gen, SerializerProvider provider) throws IOException {
    gen.writeString(chargeId.value().toString());
  }
}