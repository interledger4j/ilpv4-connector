package org.interledger.connector.link.events;

import com.google.common.collect.Maps;
import org.immutables.value.Value.Default;
import org.interledger.connector.link.Link;

import java.util.Map;

/**
 * A parent interface for all link events.
 */
public interface LinkEvent {

  /**
   * Accessor for the DataLink that emitted this event.
   */
  Link<?> link();

  /**
   * Custom properties that can be added to any DataLink event.
   *
   * @return
   */
  @Default
  default Map<String, Object> customSettings() {
    return Maps.newConcurrentMap();
  }

}