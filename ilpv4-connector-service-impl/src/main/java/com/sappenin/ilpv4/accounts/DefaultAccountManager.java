package com.sappenin.ilpv4.accounts;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.sappenin.ilpv4.connector.routing.Peer;
import com.sappenin.ilpv4.model.IlpRelationship;
import com.sappenin.ilpv4.model.settings.AccountSettings;
import com.sappenin.ilpv4.model.settings.ConnectorSettings;
import com.sappenin.ilpv4.plugins.btp.BtpPlugin;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerProtocolException;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.plugin.lpiv2.ImmutablePluginSettings;
import org.interledger.plugin.lpiv2.Plugin;
import org.interledger.plugin.lpiv2.PluginSettings;
import org.interledger.plugin.lpiv2.SimulatedChildPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The default implementation of {@link AccountManager}.
 */
public class DefaultAccountManager implements AccountManager {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final ConnectorSettings connectorSettings;
  //private final BalanceManager
  private final Map<InterledgerAddress, AccountSettings> accounts = Maps.newConcurrentMap();
  private final Map<InterledgerAddress, Plugin> plugins;
  private final Map<InterledgerAddress, Peer> peers;

  // A Connector can have multiple accounts of type `parent`, but only one can be the primary account, e.g., for
  // purposes of IL-DCP and other protocols.

  // TODO: Just use an ILP address here?
  private Optional<AccountSettings> primaryParentAccountSettings = Optional.empty();

  public DefaultAccountManager(final ConnectorSettings connectorSettings) {
    this.connectorSettings = Objects.requireNonNull(connectorSettings);
    this.plugins = Maps.newConcurrentMap();
    this.peers = Maps.newConcurrentMap();
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
  public void add(final AccountSettings account) {
    Objects.requireNonNull(account);

    // Set the primary parent-account, but only if it hasn't been set.
    if (account.getRelationship() == IlpRelationship.PARENT && !primaryParentAccountSettings.isPresent()) {
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

      // Try to connect to the account...
      this.getOrCreatePlugin(account.getInterledgerAddress()).connect();
    } catch (RuntimeException e) {
      // If any exception is thrown, then remove the peer and any plugins...
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

      if (accountSettings.getRelationship() == IlpRelationship.PARENT) {
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

  @Override
  public Stream<AccountSettings> getAllAccountSettings() {
    return accounts.values().stream();
  }

  public BigInteger getAccountBalance(InterledgerAddress accountAddress) {
    throw new RuntimeException("Balance Tracket not yet implemented!");
  }

  @Override
  public InterledgerAddress constructChildAddress(InterledgerAddress interledgerAddress) {
    Objects.requireNonNull(interledgerAddress);

    return this.connectorSettings.getIlpAddress().with(interledgerAddress.getValue());
  }

  @Override
  public Plugin getOrCreatePlugin(final InterledgerAddress peerAccountAddress) {
    Objects.requireNonNull(peerAccountAddress);

    final AccountSettings accountSettings = this.getAccountSettings(peerAccountAddress)
      .orElseThrow(() -> new InterledgerProtocolException(
        InterledgerRejectPacket.builder()
          .triggeredBy(this.connectorSettings.getIlpAddress())
          .code(InterledgerErrorCode.F02_UNREACHABLE)
          .message(String.format("Tried to get a Plugin for non-existent account: %s", peerAccountAddress))
          .build())
      );

    // Return the already constructed Plugin, or attempt to construct a new one...
    return this.getPlugin(peerAccountAddress)
      .orElseGet(() -> {
        final Plugin plugin = this.constructPlugin(
          ImmutablePluginSettings.builder()
            .pluginType(accountSettings.getPluginType())
            .peerAccount(peerAccountAddress)
            .localNodeAddress(this.connectorSettings.getIlpAddress())
            .build()
        );
        this.setPlugin(peerAccountAddress, plugin);
        return plugin;
      });
  }

  @Override
  public Optional<Plugin> getPlugin(final InterledgerAddress accountAddress) {
    Objects.requireNonNull(accountAddress);
    return Optional.ofNullable(this.plugins.get(accountAddress));
  }

  @Override
  public void setPlugin(final InterledgerAddress accountAddress, final Plugin plugin) {
    Objects.requireNonNull(accountAddress);
    Objects.requireNonNull(plugin);
    if (plugins.putIfAbsent(accountAddress, plugin) != null) {
      throw new RuntimeException(
        String.format("Plugin already exists with InterledgerAddress: %s", accountAddress)
      );
    }
  }

  @VisibleForTesting
  protected Plugin constructPlugin(final PluginSettings pluginSettings) {
    Objects.requireNonNull(pluginSettings);

    switch (pluginSettings.getPluginType().value()) {
      case SimulatedChildPlugin.PLUGIN_TYPE: {
        return new SimulatedChildPlugin(pluginSettings);
      }
      case BtpPlugin.PLUGIN_TYPE: {
        throw new RuntimeException("Not yet implemented!");
      }
      default: {
        throw new RuntimeException(String.format("Unsupported PluginType: %s", pluginSettings.getPluginType()));
      }
    }
  }

  /**
   * Helper method to disconnect a {@link Peer} based upon its Interledger Address.
   *
   * @param accountSettings The {@link AccountSettings} to disconnect.
   */
  private void disconnectAccount(final AccountSettings accountSettings) {
    Optional.of(accountSettings)
      .map(AccountSettings::getInterledgerAddress)
      .map(this::getOrCreatePlugin)
      .ifPresent(Plugin::disconnect);
  }
}
