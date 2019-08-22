package org.interledger.connector.jackson.modules;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.link.LinkId;

import java.io.IOException;

/**
 * Jackson serializer {@link AccountId}.
 */
public class LinkIdSerializer extends StdScalarSerializer<LinkId> {

  public LinkIdSerializer() {
    super(LinkId.class, false);
  }

  @Override
  public void serialize(LinkId linkId, JsonGenerator gen, SerializerProvider provider) throws IOException {
    gen.writeString(linkId.value());
  }
}
