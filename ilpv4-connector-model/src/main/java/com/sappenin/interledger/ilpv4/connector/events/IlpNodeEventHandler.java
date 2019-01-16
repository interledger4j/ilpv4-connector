package com.sappenin.interledger.ilpv4.connector.events;

import java.util.Objects;

/**
 * A helper class for mapping an instance {@link IlpNodeEvent} according to the actual polymorphic type of the passed-in
 * instantiated object.
 */
public abstract class IlpNodeEventHandler {

  /**
   * Handle the supplied {@code event} in a type-safe manner.
   *
   * @param event The generic {@link IlpNodeEvent} to responsd to.
   */
  public final void handle(final IlpNodeEvent event) {
    Objects.requireNonNull(event);

    if (PluginConstructedEvent.class.isAssignableFrom(event.getClass())) {
      handlePluginConstructedEvent((PluginConstructedEvent) event);
    } else {
      throw new RuntimeException(String.format("Unsupported IlpNodeEvent Type: %s", event.getClass()));
    }
  }

  /**
   * Handle the event as a {@link PluginConstructedEvent}.
   *
   * @param event A  {@link PluginConstructedEvent} to be responded to.
   */
  protected abstract void handlePluginConstructedEvent(final PluginConstructedEvent event);

}
