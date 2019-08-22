package com.sappenin.interledger.ilpv4.connector.links;

import com.sappenin.interledger.ilpv4.connector.links.loopback.LoopbackLink;
import com.sappenin.interledger.ilpv4.connector.links.ping.PingLoopbackLink;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.link.LinkSettings;
import org.interledger.connector.link.blast.BlastLink;
import org.interledger.connector.link.blast.BlastLinkSettings;

import java.util.Objects;

/**
 * The default {@link LinkSettingsFactory}.
 */
public class DefaultLinkSettingsFactory implements LinkSettingsFactory {

  @Override
  public LinkSettings construct(final AccountSettings accountSettings) {
    Objects.requireNonNull(accountSettings);
    switch (accountSettings.getLinkType().value().toUpperCase()) {
      case BlastLink.LINK_TYPE_STRING: {
        return BlastLinkSettings.fromCustomSettings(accountSettings.getCustomSettings()).build();
      }
      case LoopbackLink.LINK_TYPE_STRING: {
        return LinkSettings.builder().customSettings(accountSettings.getCustomSettings())
          .linkType(LoopbackLink.LINK_TYPE).build();
      }
      case PingLoopbackLink.LINK_TYPE_STRING: {
        return LinkSettings.builder().customSettings(accountSettings.getCustomSettings())
          .linkType(PingLoopbackLink.LINK_TYPE).build();
      }
      default: {
        throw new IllegalArgumentException("Unsupported LinkType: " + accountSettings.getLinkType());
      }
    }
  }
}