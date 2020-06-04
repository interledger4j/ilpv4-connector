package org.interledger.connector.jackson.modules;

import org.interledger.openpayments.CorrelationId;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

import java.io.IOException;

/**
 * Jackson serializer {@link CorrelationId}.
 */
public class CorrelationIdSerializer extends StdScalarSerializer<CorrelationId> {

  public CorrelationIdSerializer() {
    super(CorrelationId.class, false);
  }

  @Override
  public void serialize(CorrelationId correlationId, JsonGenerator gen, SerializerProvider provider) throws IOException {
    gen.writeString(correlationId.value());
  }
}
