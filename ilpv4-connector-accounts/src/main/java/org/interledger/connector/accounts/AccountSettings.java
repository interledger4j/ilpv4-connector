package org.interledger.connector.accounts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import org.interledger.connector.link.Link;
import org.interledger.connector.link.LinkType;
import org.interledger.core.InterledgerAddress;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Tracks settings for a given Connector <tt>account</tt>, which is a mechanism for tracking fungible asset value
 * between two Interledger nodes, using a single asset-type.
 */
public interface AccountSettings {

  static ImmutableAccountSettings.Builder builder() {
    return ImmutableAccountSettings.builder();
  }

  /**
   * Construct an instance of {@link ImmutableAccountSettings.Builder} from a supplied instance of {@link
   * AccountProviderSettings}.
   *
   * @param accountProviderSettings The account provider settings to source configuration from.
   *
   * @return A {@link ImmutableAccountSettings.Builder}.
   *
   * @deprecated AccountSettingsProvider will go away.
   */
  @Deprecated
  static ImmutableAccountSettings.Builder from(final AccountProviderSettings accountProviderSettings) {
    Objects.requireNonNull(accountProviderSettings);

    return builder()
      .assetScale(accountProviderSettings.getAssetScale())
      .assetCode(accountProviderSettings.getAssetCode())
      .linkType(accountProviderSettings.getLinkType())
      .accountRelationship(accountProviderSettings.getRelationship())
      .putAllCustomSettings(accountProviderSettings.getCustomSettings())
      .balanceSettings(accountProviderSettings.getBalanceSettings())
      .maximumPacketAmount(accountProviderSettings.getMaximumPacketAmount())
      .ilpAddressSegment(accountProviderSettings.getIlpAddressSegment())
      .isSendRoutes(accountProviderSettings.isSendRoutes())
      .isReceiveRoutes(accountProviderSettings.isReceiveRoutes());
  }

  /**
   * An optionally present unique identifier for this account. For example, <tt>alice</tt> or <tt>123456789</tt>. Note
   * that this is not an {@link InterledgerAddress} because an account's address is assigned when a connection is made,
   * generally using information from the client and this identifier.
   *
   * @see {@link #getIlpAddressSegment()}.
   */
  AccountId getAccountId();

  /**
   * Determines if this account is <tt>internal</tt> or <tt>external</tt>. Internal accounts are allowed to process
   * packets in the `self` and `private` prefixes, but packets from an internal account MUST not be forwarded to an
   * external address. External account are not allowed to route packets to an internal account, but are allowed to have
   * packets forwarded to other external accounts.
   *
   * @return {@code true} if this account is <tt>internal</tt>; {@code false} otherwise.
   */
  boolean isInternal();

  /**
   * Determines if connections for this Account are initiated by this Connector, or if the connections are initiated by
   * the counterparty.
   *
   * <p>For some Link types, such as BTP, a node can be either operate the Websocket client or server. In cases where
   * the Connector is the Websocket client, this value will be {@code true}. Conversely, if the Connector is operating
   * the Websocket server for an Account, then this value will be {@code false}.</p>
   *
   * <p>For other Link types, such as Ilp-over-Http, the Connector will operate both a client _and_ a server, so
   * this value will always be {@code true}.</p>
   *
   * @return {@code true} if this Connector is the connection initiator for this {@link Link}; {@code false} otherwise.
   */
  boolean isConnectionInitiator();

  /**
   * The segment that will be appended to the connector's ILP address to form this account's ILP address. Only
   * applicable to accounts with relation={@link AccountRelationship#CHILD}. By default, this will be the identifier of
   * the account, i.e. the key used in the accounts config object.
   */
  Optional<String> getIlpAddressSegment();

  /**
   * A human-readable description of this account.
   */
  default String getDescription() {
    return "";
  }

  /**
   * The relationship between this connector and a remote system, for this asset type.
   */
  AccountRelationship getAccountRelationship();

  /**
   * The {@link LinkType} that should be used for this account in order to send "data".
   */
  LinkType getLinkType();

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
  // TODO: Make this a short or a byte?! It's limited to Uint8 per IL-DCP.
  int getAssetScale();

  /**
   * The maximum amount per-packet for incoming prepare packets. The connector will reject any incoming prepare packets
   * from this account with a higher amount.
   *
   * @return The maximum packet amount allowed by this account.
   */
  Optional<Long> getMaximumPacketAmount();

  /**
   * Defines whether the connector should maintain and enforce a balance for this account.
   *
   * @return The parameters for tracking balances for this account.
   */
  AccountBalanceSettings getBalanceSettings();

  /**
   * <p>Optionally present information about how this account can be settled.</p>
   *
   * @return
   */
  Optional<SettlementEngineDetails> settlementEngineDetails();

  /**
   * Defines any rate-limiting in effect for this account.
   *
   * @return The parameters for rate-limiting this account.
   */
  AccountRateLimitSettings getRateLimitSettings();

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

  /**
   * Determines if this account is a `parent` account. If <tt>true</tt>, then the remote counterparty for this account
   * is the {@link AccountRelationship#PARENT}, and the operator of this node is the {@link AccountRelationship#CHILD}.
   *
   * @return {@code true} if this is a `parent` account; {@code false} otherwise.
   */
  default boolean isParentAccount() {
    return this.getAccountRelationship() == AccountRelationship.PARENT;
  }

  /**
   * Determines if this account is a `child` account. If <tt>true</tt>, then the remote counterparty for this account is
   * the {@link AccountRelationship#CHILD}, and the operator of this node is the {@link AccountRelationship#PARENT}.
   *
   * @return {@code true} if this is a `parent` account; {@code false} otherwise.
   */
  default boolean isChildAccount() {
    return this.getAccountRelationship() == AccountRelationship.CHILD;
  }

  /**
   * Determines if this account is a `peer` account. If <tt>true</tt>, then the remote counterparty for this account is
   * a {@link AccountRelationship#PEER}, and the operator of this node is also a {@link AccountRelationship#PEER}.
   *
   * @return {@code true} if this is a `parent` account; {@code false} otherwise.
   */
  default boolean isPeerAccount() {
    return this.getAccountRelationship() == AccountRelationship.PEER;
  }

  /**
   * Determines if this account is either a `peer` OR a 'parent` account.
   *
   * @return {@code true} if this is a `peer` OR a 'parent` account; {@code false} otherwise.
   */
  default boolean isPeerOrParentAccount() {
    return this.isPeerAccount() || this.isParentAccount();
  }

  // Purposefully not interned. Because we desire hashcode/equals to align with AccountSettingsEntity, if this class
  // were to be interned, then constructing a new instance with the same AccountId as an already interned instance
  // would simply return the old, immutable value, which would be incorrect.
  @Value.Immutable
  @Value.Modifiable
  @JsonSerialize(as = ImmutableAccountSettings.class)
  @JsonDeserialize(as = ImmutableAccountSettings.class)
  @JsonPropertyOrder({"accountId", "linkType", "ilpAddressSegment", "accountRelationship", "assetCode", "assetScale",
                       "maximumPacketAmount", "customSettings", "description", "balanceSettings", "rateLimitSettings",
                       "isConnectionInitiator", "isInternal", "isSendRoutes", "isReceiveRoutes"})
  abstract class AbstractAccountSettings implements AccountSettings {

    @Override
    public abstract AccountId getAccountId();

    @Override
    public abstract LinkType getLinkType();

    @Override
    @Value.Default
    public boolean isInternal() {
      return false;
    }

    @Override
    @Value.Default
    public boolean isConnectionInitiator() {
      return false;
    }

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
    @JsonSerialize(as = ImmutableAccountBalanceSettings.class)
    @JsonDeserialize(as = ImmutableAccountBalanceSettings.class)
    public AccountBalanceSettings getBalanceSettings() {
      return AccountBalanceSettings.builder().build();
    }

    @Value.Default
    @Override
    @JsonSerialize(as = ImmutableAccountRateLimitSettings.class)
    @JsonDeserialize(as = ImmutableAccountRateLimitSettings.class)
    public AccountRateLimitSettings getRateLimitSettings() {
      return AccountRateLimitSettings.builder().build();
    }

    @Override
    public abstract Optional<SettlementEngineDetails> settlementEngineDetails();

    @Override
    @JsonIgnore
    @Value.Derived
    public boolean isParentAccount() {
      return this.getAccountRelationship() == AccountRelationship.PARENT;
    }

    @Override
    @JsonIgnore
    @Value.Derived
    public boolean isChildAccount() {
      return this.getAccountRelationship() == AccountRelationship.CHILD;
    }

    @Override
    @JsonIgnore
    @Value.Derived
    public boolean isPeerAccount() {
      return this.getAccountRelationship() == AccountRelationship.PEER;
    }

    @Override
    @Value.Derived
    @JsonIgnore
    public boolean isPeerOrParentAccount() {
      return this.isPeerAccount() || this.isParentAccount();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      AccountSettings accountSettings = (AccountSettings) o;

      return getAccountId().equals(accountSettings.getAccountId());
    }

    @Override
    public int hashCode() {
      return getAccountId().hashCode();
    }
  }
}
