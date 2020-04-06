package org.interledger.connector.opay.jackson.modules;

import org.interledger.connector.opay.InvoiceId;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

import java.io.IOException;

/**
 * Jackson serializer {@link InvoiceId}.
 */
public class InvoiceIdSerializer extends StdScalarSerializer<InvoiceId> {

  public InvoiceIdSerializer() {
    super(InvoiceId.class, false);
  }

  @Override
  public void serialize(InvoiceId invoiceId, JsonGenerator gen, SerializerProvider provider) throws IOException {
    gen.writeString(invoiceId.value().toString());
  }
}
