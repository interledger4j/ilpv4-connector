package com.sappenin.interledger.ilpv4.connector.model.settings;

import org.immutables.value.Value;
import org.interledger.core.InterledgerAddress;
import org.interledger.plugin.lpiv2.PluginType;
import org.interledger.plugin.lpiv2.settings.PluginSettings;

import java.math.BigInteger;
import java.util.Optional;

/**
 * A unit of measurement between two Interledger nodes which tracks value using a single asset type.
 */
@Value.Immutable(intern = true)
@Value.Modifiable
public interface AccountSettings {

  /**
   * The ILP Address of this Account (should always match the remote address in the plugin settings).
   */
  @Value.Derived
  default InterledgerAddress getInterledgerAddress() {
    return this.getPluginSettings().getPeerAccountAddress();
  }

  /**
   * A human-readable description of this account.
   */
  @Value.Default
  default String getDescription() {
    return "";
  }

  /**
   * The relationship between this connector and a remote system, for this asset type.
   */
  @Value.Default
  default AccountRelationship getRelationship() {
    return AccountRelationship.PARENT;
  }

  /**
   * @return The {@link PluginType} that should be used for this account.
   */
  PluginSettings getPluginSettings();

  /**
   * Defines routing characteristics for this account.
   *
   * @return An instance of {@link RouteBroadcastSettings}.
   */
  @Value.Default
  default RouteBroadcastSettings getRouteBroadcastSettings() {
    return ImmutableRouteBroadcastSettings.builder().build();
  }

  // TODO: Configured Routes (i.e., static routes).

  /**
   * Currency code or other asset identifier that will be used to select the correct rate for this account.
   */
  @Value.Default
  default String getAssetCode() {
    return "USD";
  }

  /**
   * Interledger amounts are integers, but most currencies are typically represented as # fractional units, e.g. cents.
   * This property defines how many Interledger units make # up one regular unit. For dollars, this would usually be set
   * to 9, so that Interledger # amounts are expressed in nano-dollars.
   *
   * @return
   */
  @Value.Default
  default int getAssetScale() {
    return 2;
  }

  /**
   * Defines whether the connector should maintain and enforce a balance for this account.
   *
   * @return The parameters for tracking balances for this account.
   */
  @Value.Default
  default AccountBalanceSettings getBalanceSettings() {
    return ImmutableAccountBalanceSettings.builder().build();
  }

  /**
   * The maximum amount per packet for incoming prepare packets. The connector will reject any incoming prepare packets
   * from this account with a higher amount.
   *
   * @return The maximum packet amount allowed by this account.
   */
  Optional<BigInteger> getMaximumPacketAmount();

  /**
   * <p>Defines the type of relationship between two ILP nodes for a given account, from the perspective of the Node
   * operating the link.</p>
   *
   * <p>Each link will have one of three relationship-types that reflect how the link is related to the peer
   * on the other side of the link. These types include <tt>peer</tt>, <tt>parent</tt> or <tt>child</tt>.</p>
   *
   * <p>The Interledger network graph is organized in a tiered hierarchy, similar to the Internet, reflecting these
   * relationships. Large, high volume nodes are peered with one another to form the backbone of the network, using the
   * relationship type {@link AccountRelationship#PEER}. Smaller nodes will have links to these "tier 1" nodes and the
   * link will be of type {@link AccountRelationship#CHILD}, from the perspective of the tier 1 node; From the
   * perspective of the smaller node, the type will be {@link AccountRelationship#PARENT}.
   *
   * <p>A node MUST only have one link of type parent or, if it has multiple, only one configured to use the IL-DCP
   * protocol upon establishing the link, to request an address from the parent.</p>
   *
   * <p>A node that has links of type child must host an IL-DCP service to allow the nodes on those links to request
   * addresses. Generally these will be sub-addresses of the node's own address however this is not a requirement.</p>
   */
  enum AccountRelationship {

    /**
     * Indicates that a link is for a node that is the `parent` of the node operating the link.
     */
    PARENT(0),

    /**
     * Indicates that a link is for a node that is the `peer` of the node operating the link.
     */
    PEER(1),

    /**
     * Indicates that a link is for a node that is the `child` of the node operating the link.
     */
    CHILD(2),

    /**
     * @deprecated May be removed if unused.
     */
    @Deprecated
    LOCAL(3);

    private int weight;

    AccountRelationship(final int weight) {
      this.weight = weight;
    }

    /**
     * The weight of this relationship.
     */
    public int getWeight() {
      return weight;
    }
  }

}