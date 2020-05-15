package org.interledger.connector.localsend;

import org.interledger.connector.accounts.AccountId;
import org.interledger.core.SharedSecret;
import org.interledger.link.LinkSettings;
import org.interledger.link.LinkType;

import org.immutables.value.Value;
import org.immutables.value.Value.Derived;

/**
 * An extension of {@link LinkSettings} for {@link LocalPacketSwitchLink}. An instance of this class is created for
 * every local send operation.
 */
@Value.Immutable
public interface LocalPacketSwitchLinkSettings extends LinkSettings {

  static ImmutableLocalPacketSwitchLinkSettings.Builder builder() {
    return ImmutableLocalPacketSwitchLinkSettings.builder();
  }

  @Override
  default LinkType getLinkType() {
    return LocalPacketSwitchLink.LINK_TYPE;
  }

  /**
   * Incoming account id
   * @return
   */
  AccountId accountId();

  SharedSecret sharedSecret();

  @Value.Immutable
  abstract class AbstractLocalPacketSwitchLinkSettings implements LocalPacketSwitchLinkSettings {
    @Derived
    @Override
    public LinkType getLinkType() {
      return LocalPacketSwitchLink.LINK_TYPE;
    }
  }

}
