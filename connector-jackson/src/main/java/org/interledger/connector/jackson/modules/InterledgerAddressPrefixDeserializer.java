package org.interledger.connector.jackson.modules;

import org.interledger.core.InterledgerAddressPrefix;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

public class InterledgerAddressPrefixDeserializer extends StdDeserializer<InterledgerAddressPrefix> {

  public InterledgerAddressPrefixDeserializer() {
    super(InterledgerAddressPrefix.class);
  }

  @Override
  public InterledgerAddressPrefix deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    return InterledgerAddressPrefix.of(p.getText());
  }
}
