package com.sappenin.interledger.ilpv4.connector.links.filters;

import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.link.Link;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

public class DefaultLinkFilterChain implements LinkFilterChain {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultLinkFilterChain.class);

  private final List<LinkFilter> linkFilters;
  private final Link link;
  // The index of the filter to call next...
  private int _filterIndex;

  /**
   * A chain of filters that are applied to a packet request before sending the packet onto an outbound {@link Link}.
   *
   * @param linkFilters
   * @param outboundLink The {@link Link} that a Packet Switch will forward a packet onto (this link is the `next-hop`
   *                     as determined by the routing table inside of the Packet Switch).
   */
  public DefaultLinkFilterChain(final List<LinkFilter> linkFilters, final Link outboundLink) {
    this.linkFilters = Objects.requireNonNull(linkFilters);
    this.link = Objects.requireNonNull(outboundLink);
  }

  @Override
  public InterledgerResponsePacket doFilter(
    final AccountSettings destinationAccountSettings, final InterledgerPreparePacket preparePacket
  ) {

    Objects.requireNonNull(destinationAccountSettings);
    Objects.requireNonNull(preparePacket);

    if (this._filterIndex < this.linkFilters.size()) {
      return linkFilters.get(_filterIndex++).doFilter(destinationAccountSettings, preparePacket, this);
    } else {
      LOGGER.debug(
        "Sending outbound ILP Prepare. destinationAccountSettings: {}; link={}; packet={};",
        destinationAccountSettings, link, preparePacket
      );
      return link.sendPacket(preparePacket);
    }
  }
}
