package com.sappenin.interledger.ilpv4.connector.routing;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountSettings;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * <p>Centralizes all Route broadcasting logic in order to coordinate routing updates across peer-account
 * connections.</p>
 *
 * <p>Note that most of the details of route-management are by instances of {@link CcpSender} and {@link CcpReceiver},
 * so this interface manages instances of {@link RoutableAccount}, which hold both.</p>
 */
public interface RouteBroadcaster {

  /**
   * Registers an account with this route broadcaster, but only if the account exists.
   *
   * @param accountId The {@link AccountId} of the account to register with this service.
   *
   * @return A {@link RoutableAccount} that was created during registration, or {@link Optional#empty()} if the account
   * didn't exist or the account was not eligible to send and receive routing updates.
   */
  Optional<RoutableAccount> registerCcpEnabledAccount(final AccountId accountId);

  /**
   * Registers an account with this route broadcaster.
   *
   * @param accountSettings The {@link AccountSettings} for the account to register.
   *
   * @return A {@link RoutableAccount} that was created during registration, or {@link Optional#empty()} if the account
   * was not eligible to send and receive routing updates.
   */
  Optional<RoutableAccount> registerCcpEnabledAccount(final AccountSettings accountSettings);

  /**
   * Accessor for the optionally-present {@link RoutableAccount} that is being used to broadcast route information to a
   * peer.
   *
   * @param accountId A {@link AccountId} that uniquely identifies the account used to broadcast routing updates.
   *
   * @return A {@link RoutableAccount}, if present.
   */
  Optional<RoutableAccount> getCcpEnabledAccount(final AccountId accountId);

  /**
   * Obtain a {@link Stream} of {@link RoutableAccount}.
   *
   * @return
   */
  Stream<RoutableAccount> getAllCcpEnabledAccounts();
}
