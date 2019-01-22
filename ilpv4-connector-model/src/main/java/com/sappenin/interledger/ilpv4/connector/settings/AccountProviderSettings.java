package com.sappenin.interledger.ilpv4.connector.settings;

import com.sappenin.interledger.ilpv4.connector.AccountProviderId;
import org.immutables.value.Value;
import org.interledger.core.InterledgerAddress;
import org.interledger.plugin.lpiv2.PluginSettings;
import org.interledger.plugin.lpiv2.PluginType;

import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;

/**
 * Tracks settings for a given Connector <tt>account provider</tt>, which allows a Connector to define Account
 * attributes on-the-fly for any new incoming connection that supports. Examples of an AccountProvider include a BTP
 * server, where connecting to a new account requires settings and other default information that very account sould
 * support.
 */
public interface AccountProviderSettings {

  static ImmutableAccountProviderSettings.Builder builder() {
    return ImmutableAccountProviderSettings.builder();
  }

  /**
   * An optionally present unique identifier for this account. For example, <tt>alice</tt> or <tt>123456789</tt>. Note
   * that this is not an {@link InterledgerAddress} because an account's address is assigned when a connection is made,
   * generally using information from the client and this identifier.
   *
   * @see {@link #getIlpAddressSegment()}.
   */
  AccountProviderId getId();

  /**
   * A human-readable description of this account.
   */
  default String getDescription() {
    return "";
  }

  /**
   * The segment that will be appended to the connector's ILP address to form this account's ILP address. Only
   * applicable to accounts with relation={@link AccountRelationship#CHILD}. By default, this will be the identifier of
   * the account, i.e. the key used in the accounts config object.
   */
  Optional<String> getIlpAddressSegment();

  /**
   * The relationship between this connector and a remote system, for this asset type.
   */
  AccountRelationship getRelationship();

  /**
   * The {@link PluginType} that should be used for this account.
   *
   * Use {@link PluginSettingsFactory#getPluginSettings} to assemble a typed instance of {@link PluginSettings}.
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
   * The maximum amount per packet for incoming prepare packets. The connector will reject any incoming prepare packets
   * from this account with a higher amount.
   *
   * @return The maximum packet amount allowed by this account.
   */
  Optional<BigInteger> getMaximumPacketAmount();

  /**
   * Defines whether the connector should maintain and enforce a balance for this account.
   *
   * @return The parameters for tracking balances for this account.
   */
  AccountBalanceSettings getBalanceSettings();

  /**
   * Whether this account should receive and process route broadcasts from this peer. Defaults to `false` for {@link
   * AccountRelationship#CHILD} and `true` otherwise.
   */
  boolean isSendRoutes();

  /**
   * Whether this account should broadcast routes to this peer. Defaults to `false` for {@link
   * AccountRelationship#CHILD} and `true` otherwise.
   */
  boolean isReceiveRoutes();

  // TODO: `throughput`, `ratelimit`, `deduplicate`, etc.
  // See https://github.com/interledgerjs/ilp-connector#accounts

  /**
   * Additional, custom settings that any plugin can define.
   */
  Map<String, Object> getCustomSettings();

  @Value.Immutable(intern = true)
  abstract class AbstractAccountProviderSettings implements AccountProviderSettings {

    @Value.Default
    @Override
    public String getDescription() {
      return "";
    }

    @Value.Default
    @Override
    public boolean isSendRoutes() {
      return false;
    }

    @Value.Default
    @Override
    public boolean isReceiveRoutes() {
      return false;
    }

    @Value.Default
    @Override
    public AccountBalanceSettings getBalanceSettings() {
      return AccountBalanceSettings.builder().build();
    }
  }

}