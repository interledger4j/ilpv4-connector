package com.sappenin.ilpv4.model.settings;

import com.sappenin.ilpv4.model.IlpRelationship;
import org.immutables.value.Value;
import org.interledger.core.InterledgerAddress;
import org.interledger.plugin.lpiv2.PluginType;

import java.math.BigInteger;
import java.util.Optional;

/**
 * Defines settings for a particular account, which tracks value between this connector and a remote peer, using a
 * single asset type.
 */
@Value.Immutable(intern = true)
@Value.Modifiable
public interface AccountSettings {

  /**
   * The ILP Address of this Account.
   */
  InterledgerAddress getInterledgerAddress();

  /**
   * The relationship between this connector and a remote system, for this asset type.
   */
  @Value.Default
  default IlpRelationship getRelationship() {
    return IlpRelationship.PARENT;
  }

  /**
   * @return The {@link PluginType} that should be used for this account.
   */
  PluginType getPluginType();

  /**
   * Currency code or other asset identifier that will be used to select the correct rate for this account.
   */
  String getAssetCode();

  /**
   * Interledger amounts are integers, but most currencies are typically represented as # fractional units, e.g. cents.
   * This property defines how many Interledger units make # up one regular unit. For dollars, this would usually be set
   * to 9, so that Interledger # amounts are expressed in nano-dollars.
   *
   * @return
   */
  int getAssetScale();

  /**
   * Defines whether the connector should maintain and enforce a balance for this account.
   *
   * @return The parameters for tracking balances for this account.
   */
  AccountBalanceSettings getBalanceSettings();

  /**
   * Defines routing characteristics for this account.
   *
   * @return An instance of {@link RouteBroadcastSettings}.
   */
  RouteBroadcastSettings getRouteBroadcastSettings();

  // TODO: Configured Routes (i.e., static routes).
  



  /**
   * The maximum amount per packet for incoming prepare packets. The connector will reject any incoming prepare packets
   * from this account with a higher amount.
   *
   * @return The maximum packet amount allowed by this account.
   */
  Optional<BigInteger> getMaximumPacketAmount();
}