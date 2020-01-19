package org.interledger.connector.accounts;

import org.interledger.core.InterledgerAddress;
import org.interledger.link.Link;
import org.interledger.link.LinkType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Tracks settings for a given Connector <tt>account</tt>, which is a mechanism for tracking fungible asset value
 * between two Interledger nodes, using a single asset-type.
 */
@JsonSerialize(as = ImmutableAccountSettings.class)
@JsonDeserialize(as = ImmutableAccountSettings.class)
@JsonPropertyOrder( {"accountId", "createdAt", "modifiedAt", "description", "accountRelationship", "assetCode",
  "assetScale", "maximumPacketAmount", "linkType", "ilpAddressSegment", "connectionInitiator",
  "internal", "sendRoutes", "receiveRoutes", "balanceSettings", "rateLimitSettings",
  "settlementEngineDetails", "customSettings"})
public interface AccountSettings {

  static ImmutableAccountSettings.Builder builder() {
    return ImmutableAccountSettings.builder();
  }

  /**
   * <p>An optionally present unique identifier for this account. For example, <tt>alice</tt> or <tt>123456789</tt>.
   * Note that this is not an {@link InterledgerAddress} because an account's address is assigned when a connection is
   * made, generally using information from the client and this identifier.</p>
   *
   * <p>See {@link #ilpAddressSegment} for more details.</p>
   */
  AccountId accountId();

  /**
   * The date/time this Account was created.
   *
   * @return An {@link Instant}.
   */
  default Instant createdAt() {
    return Instant.now();
  }

  /**
   * The date/time this Account was last modified.
   *
   * @return An {@link Instant}.
   */
  default Instant modifiedAt() {
    return Instant.now();
  }

  /**
   * Determines if this account is <tt>internal</tt> or <tt>external</tt>. Internal accounts are allowed to process
   * packets in the `self` and `private` prefixes, but packets from an internal account MUST not be forwarded to an
   * external address. Likewise, external account are not allowed to be the source of packets that get forwarded to an
   * internal account, but external accounts are allowed to be the source of packets that get forwarded to other
   * external accounts. For more details on these rules, see `InterledgerAddressUtils`.
   *
   * @return {@code true} if this account is <tt>internal</tt>; {@code false} otherwise.
   */
  boolean isInternal();

  /**
   * <p>Determines if connections for this Account are initiated by this Connector, or if the connections are
   * initiated by the counterparty.</p>
   *
   * <p>For some Link types, such as BTP, a node can be either operate the Websocket client or server. In cases where
   * the Connector is the Websocket client, this value will be {@code true}. Conversely, if the Connector is operating
   * the Websocket server for an Account, then this value will be {@code false}.</p>
   *
   * <p>For other Link types, such as ILP-over-HTTP, the Connector will operate both a client _and_ a server, so
   * this value will always be {@code true}.</p>
   *
   * @return {@code true} if this Connector is the connection initiator for this {@link Link}; {@code false} otherwise.
   */
  default boolean isConnectionInitiator() {
    return true;
  }

  /**
   * The segment that will be appended to the connector's ILP address to form this account's ILP address. Only
   * applicable to accounts with relation of {@link AccountRelationship#CHILD}. By default, this will be the identifier
   * of the account.
   */
  default String ilpAddressSegment() {
    return this.accountId().value();
  }

  /**
   * A human-readable description of this account.
   */
  default String description() {
    return "";
  }

  /**
   * The relationship between this connector and a remote system, for this asset type.
   */
  AccountRelationship accountRelationship();

  /**
   * The {@link LinkType} that should be used for this account in order to send "data".
   */
  LinkType linkType();

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

  /**
   * The maximum amount per-packet for incoming prepare packets. The connector will reject any incoming prepare packets
   * from this account with a higher amount.
   *
   * @return The maximum packet amount allowed by this account.
   */
  Optional<UnsignedLong> maximumPacketAmount();

  /**
   * Defines whether the connector should maintain and enforce a balance for this account.
   *
   * @return The parameters for tracking balances for this account.
   */
  AccountBalanceSettings balanceSettings();

  /**
   * <p>Optionally present information about how this account can be settled.</p>
   *
   * @return An optionally present {@link SettlementEngineDetails}. If this value is absent, then this account does not
   *   support settlement.
   */
  Optional<SettlementEngineDetails> settlementEngineDetails();

  /**
   * Defines any rate-limiting in effect for this account.
   *
   * @return The parameters for rate-limiting this account.
   */
  AccountRateLimitSettings rateLimitSettings();

  /**
   * Indicates whether this account should send route broadcasts to this peer on the other side of this * account.
   * Defaults to `true` for accounts of type {@link AccountRelationship#CHILD} or {@link AccountRelationship#PEER} and
   * `false` otherwise.
   */
  default boolean isSendRoutes() {
    return this.isPeerAccount() || this.isChildAccount();
  }

  /**
   * Indicates whether this account should receive/accept route broadcasts from the peer on the other side of this
   * account. Defaults to `true` for accounts of type {@link AccountRelationship#PARENT} or {@link
   * AccountRelationship#PEER} and `false` otherwise.
   */
  default boolean isReceiveRoutes() {
    return this.isPeerAccount() || this.isParentAccount();
  }

  // TODO: `throughput`, `ratelimit`, etc.
  // See https://github.com/interledgerjs/ilp-connector#accounts

  /**
   * Additional, custom settings that any plugin can define.
   */
  Map<String, Object> customSettings();

  /**
   * Determines if this account is a `parent` account. If <tt>true</tt>, then the remote counterparty for this account
   * is the {@link AccountRelationship#PARENT}, and the operator of this node is the {@link AccountRelationship#CHILD}.
   *
   * @return {@code true} if this is a `parent` account; {@code false} otherwise.
   */
  default boolean isParentAccount() {
    return this.accountRelationship() == AccountRelationship.PARENT;
  }

  /**
   * Determines if this account is a `child` account. If <tt>true</tt>, then the remote counterparty for this account is
   * the {@link AccountRelationship#CHILD}, and the operator of this node is the {@link AccountRelationship#PARENT}.
   *
   * @return {@code true} if this is a `parent` account; {@code false} otherwise.
   */
  default boolean isChildAccount() {
    return this.accountRelationship() == AccountRelationship.CHILD;
  }

  /**
   * Determines if this account is a `peer` account. If <tt>true</tt>, then the remote counterparty for this account is
   * a {@link AccountRelationship#PEER}, and the operator of this node is also a {@link AccountRelationship#PEER}.
   *
   * @return {@code true} if this is a `parent` account; {@code false} otherwise.
   */
  default boolean isPeerAccount() {
    return this.accountRelationship() == AccountRelationship.PEER;
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
  @JsonPropertyOrder( {"accountId", "createdAt", "modifiedAt", "description", "accountRelationship", "assetCode",
    "assetScale", "maximumPacketAmount", "linkType", "ilpAddressSegment", "connectionInitiator",
    "internal", "sendRoutes", "receiveRoutes", "balanceSettings", "rateLimitSettings",
    "settlementEngineDetails", "customSettings"})
  abstract class AbstractAccountSettings implements AccountSettings {

    @Override
    public abstract AccountId accountId();

    @Override
    @Value.Default
    public Instant createdAt() {
      return Instant.now();
    }

    @Override
    @Value.Default
    public Instant modifiedAt() {
      return Instant.now();
    }

    @Value.Default
    @Override
    public String description() {
      return "";
    }

    @Override
    public abstract LinkType linkType();

    @Override
    @Value.Default
    @JsonProperty("internal")
    public boolean isInternal() {
      return false;
    }

    @Override
    @Value.Default
    @JsonProperty("connectionInitiator")
    public boolean isConnectionInitiator() {
      return true;
    }

    @Value.Default
    @Override
    public String ilpAddressSegment() {
      return this.accountId().value();
    }

    @Value.Default
    @Override
    @JsonProperty("sendRoutes")
    public boolean isSendRoutes() {
      return this.isPeerAccount() || this.isChildAccount();
    }

    @Value.Default
    @Override
    @JsonProperty("receiveRoutes")
    public boolean isReceiveRoutes() {
      return this.isPeerAccount() || this.isParentAccount();
    }

    @Value.Default
    @Override
    @JsonSerialize(as = ImmutableAccountBalanceSettings.class)
    @JsonDeserialize(as = ImmutableAccountBalanceSettings.class)
    public AccountBalanceSettings balanceSettings() {
      return AccountBalanceSettings.builder().build();
    }

    @Value.Default
    @Override
    @JsonSerialize(as = ImmutableAccountRateLimitSettings.class)
    @JsonDeserialize(as = ImmutableAccountRateLimitSettings.class)
    public AccountRateLimitSettings rateLimitSettings() {
      return AccountRateLimitSettings.builder().build();
    }

    @Override
    public abstract Optional<SettlementEngineDetails> settlementEngineDetails();

    @Override
    @JsonIgnore
    @Value.Derived
    public boolean isParentAccount() {
      return this.accountRelationship() == AccountRelationship.PARENT;
    }

    @Override
    @JsonIgnore
    @Value.Derived
    public boolean isChildAccount() {
      return this.accountRelationship() == AccountRelationship.CHILD;
    }

    @Override
    @JsonIgnore
    @Value.Derived
    public boolean isPeerAccount() {
      return this.accountRelationship() == AccountRelationship.PEER;
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

      return accountId().equals(accountSettings.accountId());
    }

    @Override
    public int hashCode() {
      return accountId().hashCode();
    }
  }
}
