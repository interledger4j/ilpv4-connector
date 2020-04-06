package org.interledger.connector.stream;

import org.interledger.connector.accounts.AccountId;
import org.interledger.link.LinkSettings;
import org.interledger.link.LinkType;

import org.immutables.value.Value;
import org.immutables.value.Value.Derived;

/**
 * An extension of {@link LinkSettings} for Tracking receiver links.
 */
public interface TrackingStreamReceiverLinkSettings extends LinkSettings {

  static ImmutableTrackingStreamReceiverLinkSettings.Builder builder() {
    return ImmutableTrackingStreamReceiverLinkSettings.builder();
  }

  @Override
  default LinkType getLinkType() {
    return TrackingStreamReceiverLink.LINK_TYPE;
  }

  AccountId accountId();

  /**
   * Currency code or other asset identifier that will be used to select the correct rate for this account.
   */
  String assetCode();

  /**
   * Interledger amounts are integers, but most currencies are typically represented as # fractional units, e.g. cents.
   * This property defines how many Interledger units make # up one regular unit. For dollars, this would usually be set
   * to 9, so that Interledger # amounts are expressed in nano-dollars.
   *
   * @return an int representing this account's asset scale.
   */
  int assetScale();

  @Value.Immutable
  abstract class AbstractTrackingStreamReceiverLinkSettings implements TrackingStreamReceiverLinkSettings {

    @Derived
    @Override
    public LinkType getLinkType() {
      return TrackingStreamReceiverLink.LINK_TYPE;
    }

  }
}
