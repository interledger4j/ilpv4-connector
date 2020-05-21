package org.interledger.connector.jackson.modules;

import org.interledger.connector.opa.model.CorrelationId;
import org.interledger.connector.opa.model.InvoiceId;
import org.interledger.connector.opa.model.PaymentId;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

import java.io.IOException;

/**
 * Jackson serializer {@link InvoiceId}.
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
