package org.interledger.connector.link;

import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A provider of link factories scoped by type.
 */
public class LinkFactoryProvider {

  // At runtime, there will potentially be _many_ factories depending on the LinkType (e.g., a BTP Factory, a
  // LoopbackFactory, etc).
  private final Map<LinkType, LinkFactory> linkFactories;

  public LinkFactoryProvider() {
    this(Maps.newConcurrentMap());
  }

  public LinkFactoryProvider(final Map<LinkType, LinkFactory> linkFactories) {
    this.linkFactories = Objects.requireNonNull(linkFactories);
  }

  public Optional<LinkFactory> getLinkFactory(final LinkType linkType) {
    return Optional.ofNullable(this.linkFactories.get(linkType));
  }

  public LinkFactory registerLinkFactory(final LinkType linkType, final LinkFactory linkFactory) {
    return this.linkFactories.put(linkType, linkFactory);
  }
}
