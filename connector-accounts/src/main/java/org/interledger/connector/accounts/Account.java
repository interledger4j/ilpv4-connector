package org.interledger.connector.accounts;

import org.immutables.value.Value;
import org.interledger.connector.link.Link;
import org.interledger.core.InterledgerAddress;

/**
 * The main object for tracking an account. This interface contains information that is not known until a peer connects
 * using this account, such as the account-address. All other statically-configured information is found in an {@link
 * AccountSettings} contained in this object, which depending on the implementation, may be runtime configurable.
 *
 * @deprecated Use `AccountSettingsRepository` or `LinkManager` instead.
 */
@Value.Immutable(intern = true)
@Deprecated
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
  @Deprecated
  default AccountId getId() {
    return getAccountSettings().getAccountId();
  }

  /**
   * The original settings used to construct this account instance.
   */
  @Deprecated
  AccountSettings getAccountSettings();

  @Deprecated
  Link<?> getLink();
}
