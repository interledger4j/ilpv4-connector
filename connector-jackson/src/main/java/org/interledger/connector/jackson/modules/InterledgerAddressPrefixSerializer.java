package org.interledger.connector.jackson.modules;

import org.interledger.core.InterledgerAddressPrefix;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

import java.io.IOException;

public class InterledgerAddressPrefixSerializer extends StdScalarSerializer<InterledgerAddressPrefix> {

  public InterledgerAddressPrefixSerializer() {
    super(InterledgerAddressPrefix.class, false);
  }

  @Override
  public void serialize(InterledgerAddressPrefix value, JsonGenerator gen,
                        SerializerProvider provider) throws IOException {
    gen.writeString(value.getValue());
  }
}
