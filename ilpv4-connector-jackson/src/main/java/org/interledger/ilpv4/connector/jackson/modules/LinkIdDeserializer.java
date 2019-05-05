package org.interledger.ilpv4.connector.jackson.modules;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.interledger.connector.link.LinkId;

import java.io.IOException;

/**
 * Jackson deserializer for {@link LinkId}.
 */
public class LinkIdDeserializer extends StdDeserializer<LinkId> {

  protected LinkIdDeserializer() {
    super(LinkId.class);
  }

  @Override
  public LinkId deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
    return LinkId.of(jsonParser.getText());
  }
}