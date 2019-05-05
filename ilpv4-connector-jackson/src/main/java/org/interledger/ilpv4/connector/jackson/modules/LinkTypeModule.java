package org.interledger.ilpv4.connector.jackson.modules;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.interledger.connector.link.LinkType;


/**
 * A Jackson {@linkType SimpleModule} for serializing and deserializing instances of {@link LinkType}.
 */
public class LinkTypeModule extends SimpleModule {

  private static final String NAME = "LinkTypeModule";

  /**
   * No-args Constructor.
   */
  public LinkTypeModule() {
    super(
      NAME,
      new Version(1, 0, 0, null, "org.interledger.ilpv4.connector",
        "link-type")
    );

    addSerializer(LinkType.class, new LinkTypeSerializer());
    addDeserializer(LinkType.class, new LinkTypeDeserializer());
  }
}