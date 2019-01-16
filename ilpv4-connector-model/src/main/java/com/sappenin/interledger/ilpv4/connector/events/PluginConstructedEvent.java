package com.sappenin.interledger.ilpv4.connector.events;

import com.sappenin.interledger.ilpv4.connector.Account;
import org.immutables.value.Value;
import org.interledger.plugin.lpiv2.Plugin;

// TODO: Fix this -- is it correct to have the type be Account?
/**
 * Emitted after a {@link Plugin} instance is constructed for a particular account.
 */
public interface PluginConstructedEvent extends IlpNodeEvent<Account> {

  static ImmutablePluginConstructedEvent.Builder builder() {
    return ImmutablePluginConstructedEvent.builder();
  }

  /**
   * Accessor for the plugin that was newly constructed.
   */
  default Account getAccount() {
    return this.getObject();
  }

  @Value.Immutable
  abstract class AbstractPluginConstructedEvent implements
    PluginConstructedEvent {

  }

}