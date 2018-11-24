package com.sappenin.interledger.ilpv4.connector.accounts;

import com.google.common.collect.Maps;
import com.sappenin.interledger.ilpv4.connector.model.settings.ConnectorSettings;
import org.interledger.core.InterledgerAddress;
import com.sappenin.interledger.ilpv4.connector.model.settings.AccountSettings;
import org.interledger.plugin.lpiv2.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * The default implementation of {@link AccountManager}.
 *
 * WARNING: This Account manager should never know anything about routing.
 */
public class DefaultAccountManager implements AccountManager {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final Supplier<ConnectorSettings> connectorSettings;
  private final Map<InterledgerAddress, AccountSettings> accounts = Maps.newConcurrentMap();

  private final PluginManager pluginManager;

  // A Connector can have multiple accounts of type `parent`, but only one can be the primary account, e.g., for
  // purposes of IL-DCP and other protocols.
  // TODO: Just use an ILP address here?
  private Optional<AccountSettings> primaryParentAccountSettings = Optional.empty();

  /**
   * Required-args Constructor.
   */
  public DefaultAccountManager(final Supplier<ConnectorSettings> connectorSettings, final PluginManager pluginManager) {
    this.connectorSettings = Objects.requireNonNull(connectorSettings);
    this.pluginManager = pluginManager;
  }

  /**
   * Called just before this Peer Manager will be destroyed (i.e., disconnect all peers), via Spring naming convention
   * when the Spring container is shut down.
   */
  @Override
  public void shutdown() {
    // Attempt to disconnect from each Account configured for this Peer.
    this.accounts.values().stream().forEach(this::disconnectAccount);
  }

  @Override
  public Plugin add(final AccountSettings account) {
    Objects.requireNonNull(account);

    // Set the primary parent-account, but only if it hasn't been set.
    if (account.getRelationship() == AccountSettings.AccountRelationship.PARENT &&
      !primaryParentAccountSettings.isPresent()) {
      // Set the parent peer, if it exists.
      this.setPrimaryParentAccountSettings(account);
    }

    try {
      if (accounts.putIfAbsent(account.getInterledgerAddress(), account) != null) {
        throw new RuntimeException(
          String.format("Account may only be configured once! InterledgerAddress: %s",
            account.getInterledgerAddress())
        );
      }

      // Try to connect to the account...block until the connection is made, or throw an exception...
      final Plugin plugin = this.getPluginManager().getPlugin(account.getInterledgerAddress())
        .orElseGet(() -> this.getPluginManager().createPlugin(account));

      return plugin;
    } catch (Exception e) {
      // If any exception is thrown, then removeEntry the peer and any plugins...
      this.remove(account.getInterledgerAddress());
      throw new RuntimeException(e);
    }
  }


  @Override
  public void remove(final InterledgerAddress interledgerAddress) {
    Objects.requireNonNull(interledgerAddress);

    this.getAccountSettings(interledgerAddress).ifPresent(accountSettings -> {
      // Disconnect the account...
      this.disconnectAccount(accountSettings);

      if (accountSettings.getRelationship() == AccountSettings.AccountRelationship.PARENT) {
        // Set the parent peer, if it exists.
        this.unsetPrimaryParentAccountSettings();
      }
    });

    this.accounts.remove(interledgerAddress);
  }

  @Override
  public Optional<AccountSettings> getPrimaryParentAccountSettings() {
    return this.primaryParentAccountSettings;
  }

  /**
   * Allows only a single thread to set a peer at a time, ensuring that only one will win.
   */
  private synchronized void setPrimaryParentAccountSettings(final AccountSettings accountSettings) {
    Objects.requireNonNull(accountSettings);

    if (this.primaryParentAccountSettings.isPresent()) {
      throw new RuntimeException("Only a single Primary Parent Account may be configured for a Connector!");
    }

    logger.info("Primary Parent Account: {}", accountSettings.getInterledgerAddress().getValue());
    this.primaryParentAccountSettings = Optional.of(accountSettings);
  }

  /**
   * Allows only a single thread to unset a peer at a time, ensuring that only one will win.
   */
  private synchronized void unsetPrimaryParentAccountSettings() {
    this.primaryParentAccountSettings = Optional.empty();
  }

  @Override
  public Optional<AccountSettings> getAccountSettings(InterledgerAddress interledgerAddress) {
    return Optional.ofNullable(this.accounts.get(interledgerAddress));
  }

  /**
   * Accessor for the manager that controls all plugins for any accounts defined in this manager.
   *
   * @return
   */
  @Override
  public PluginManager getPluginManager() {
    return this.pluginManager;
  }

  @Override
  public Stream<AccountSettings> getAllAccountSettings() {
    return accounts.values().stream();
  }

  public BigInteger getAccountBalance(InterledgerAddress accountAddress) {
    throw new RuntimeException("Balance Tracket not yet implemented!");
  }

  @Override
  public InterledgerAddress toChildAddress(InterledgerAddress interledgerAddress) {
    Objects.requireNonNull(interledgerAddress);

    return this.connectorSettings.get().getIlpAddress().with(interledgerAddress.getValue());
  }

  /**
   * Helper method to disconnect an account based upon its Interledger Address.
   *
   * @param accountSettings The {@link AccountSettings} to disconnect.
   */
  private void disconnectAccount(final AccountSettings accountSettings) {
    Optional.of(accountSettings)
      .map(AccountSettings::getInterledgerAddress)
      .map(address -> this.getPluginManager().getPlugin(address))
      .filter(Optional::isPresent)
      .map(Optional::get)
      .ifPresent(Plugin::disconnect);
  }
}
