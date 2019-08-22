package org.interledger.connector.jackson.modules;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.link.LinkType;

import java.io.IOException;

/**
 * Jackson serializer {@link AccountId}.
 */
public class LinkTypeSerializer extends StdScalarSerializer<LinkType> {

  public LinkTypeSerializer() {
    super(LinkType.class, false);
  }

  @Override
  public void serialize(LinkType linkType, JsonGenerator gen, SerializerProvider provider) throws IOException {
    gen.writeString(linkType.value());
  }
}
