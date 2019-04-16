package org.interledger.connector.accounts;

import org.immutables.value.Value;
import org.interledger.connector.link.Link;
import org.interledger.core.InterledgerAddress;

/**
 * The main object for tracking an account. This interface contains information that is not known until a peer connects
 * using this account, such as the account-address. All other statically-configured information is found in an {@link
 * AccountSettings} contained in this object, which depending on the implementation, may be runtime configurable.
 */
@Value.Immutable(intern = true)
public interface Account {

  static ImmutableAccount.Builder builder() {
    return ImmutableAccount.builder();
  }

  /**
   * The unique identifier of this account. For example, <tt>alice</tt> or <tt>123456789</tt>. Note that this is not an
   * {@link InterledgerAddress} because an account's address is assigned when a connection is made, generally using
   * information from the client.
   *
   * @see {@link #getAccountSettings#getIlpAddressSegment()}.
   */
  @Value.Derived
  default AccountId getId() {
    return getAccountSettings().getAccountId();
  }

  /**
   * The original settings used to construct this account instance.
   */
  AccountSettings getAccountSettings();

  Link<?> getLink();

  /**
   * Determines if this account is a `parent` account. If <tt>true</tt>, then the remote counterparty for this account
   * is the {@link AccountRelationship#PARENT}, and the operator of this node is the {@link AccountRelationship#CHILD}.
   *
   * @return {@code true} if this is a `parent` account; {@code false} otherwise.
   */
  @Value.Derived
  default boolean isParentAccount() {
    return this.getAccountSettings().isParentAccount();
  }

  /**
   * Determines if this account is a `child` account. If <tt>true</tt>, then the remote counterparty for this account is
   * the {@link AccountRelationship#CHILD}, and the operator of this node is the {@link AccountRelationship#PARENT}.
   *
   * @return {@code true} if this is a `parent` account; {@code false} otherwise.
   */
  @Value.Derived
  default boolean isChildAccount() {
    return this.getAccountSettings().isChildAccount();
  }

  /**
   * Determines if this account is a `peer` account. If <tt>true</tt>, then the remote counterparty for this account is
   * a {@link AccountRelationship#PEER}, and the operator of this node is also a {@link AccountRelationship#PEER}.
   *
   * @return {@code true} if this is a `parent` account; {@code false} otherwise.
   */
  @Value.Derived
  default boolean isPeerAccount() {
    return this.getAccountSettings().isPeerAccount();
  }

  /**
   * Determines if this account is either a `peer` OR a 'parent` account.
   *
   * @return {@code true} if this is a `peer` OR a 'parent` account; {@code false} otherwise.
   */
  @Value.Derived
  default boolean isPeerOrParentAccount() {
    return this.isPeerAccount() || this.isParentAccount();
  }

}
