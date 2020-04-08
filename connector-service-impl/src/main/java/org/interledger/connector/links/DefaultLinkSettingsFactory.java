package org.interledger.connector.links;

import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.stream.TrackingStreamReceiverLink;
import org.interledger.connector.stream.TrackingStreamReceiverLinkSettings;
import org.interledger.link.LinkSettings;
import org.interledger.link.LoopbackLink;
import org.interledger.link.PingLoopbackLink;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IlpOverHttpLinkSettings;

import java.util.Objects;

/**
 * The default {@link LinkSettingsFactory}.
 */
public class DefaultLinkSettingsFactory implements LinkSettingsFactory {

  @Override
  public LinkSettings construct(final AccountSettings accountSettings) {
    Objects.requireNonNull(accountSettings);
    switch (accountSettings.linkType().value().toUpperCase()) {
      case IlpOverHttpLink.LINK_TYPE_STRING: {
        return IlpOverHttpLinkSettings.fromCustomSettings(accountSettings.customSettings()).build();
      }
      case LoopbackLink.LINK_TYPE_STRING: {
        return LinkSettings.builder().customSettings(accountSettings.customSettings())
          .linkType(LoopbackLink.LINK_TYPE).build();
      }
      case PingLoopbackLink.LINK_TYPE_STRING: {
        return LinkSettings.builder().customSettings(accountSettings.customSettings())
          .linkType(PingLoopbackLink.LINK_TYPE).build();
      }
      case TrackingStreamReceiverLink.LINK_TYPE_STRING: {
        return TrackingStreamReceiverLinkSettings.builder()
          .assetScale(accountSettings.assetScale())
          .assetCode(accountSettings.assetCode())
          .accountId(accountSettings.accountId())
          .build();
      }
      default: {
        throw new IllegalArgumentException("Unsupported LinkType: " + accountSettings.linkType());
      }
    }
  }
}
