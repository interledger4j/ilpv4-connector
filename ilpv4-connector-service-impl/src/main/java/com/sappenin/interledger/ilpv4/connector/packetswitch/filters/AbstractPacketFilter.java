package com.sappenin.interledger.ilpv4.connector.packetswitch.filters;

import com.sappenin.interledger.ilpv4.connector.packetswitch.PacketRejector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * An abstract implementation of {@link PacketSwitchFilter} that contains common logic.
 */
public abstract class AbstractPacketFilter implements PacketSwitchFilter {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  protected final PacketRejector packetRejector;

  public AbstractPacketFilter(final PacketRejector packetRejector) {
    this.packetRejector = Objects.requireNonNull(packetRejector);
  }

}
