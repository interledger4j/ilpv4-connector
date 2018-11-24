package com.sappenin.interledger.ilpv4.connector.model.events;

import org.immutables.value.Value;
import org.interledger.plugin.lpiv2.Plugin;

/**
 * Emitted after a {@link Plugin} instance is constructed.
 */
public interface PluginConstructedEvent extends IlpNodeEvent<Plugin<?>> {

  static ImmutablePluginConstructedEvent.Builder builder() {
    return ImmutablePluginConstructedEvent.builder();
  }

  /**
   * Accessor for the plugin that was newly constructed.
   */
  default Plugin<?> getPlugin() {
    return this.getObject();
  }

  @Value.Immutable
  abstract class AbstractPluginConstructedEvent implements
    PluginConstructedEvent {

  }

}