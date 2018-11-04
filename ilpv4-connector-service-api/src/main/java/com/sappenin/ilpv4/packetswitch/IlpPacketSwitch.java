package com.sappenin.ilpv4.packetswitch;

import com.sappenin.ilpv4.packetswitch.filters.SendDataFilter;
import com.sappenin.ilpv4.packetswitch.filters.SendDataFilterChain;
import com.sappenin.ilpv4.packetswitch.filters.SendMoneyFilter;
import org.interledger.core.*;
import org.interledger.plugin.lpiv2.Plugin;

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;

/**
 * <p>A collection of PacketHandlers that are applied to every Packet that traverses the Connector's routing
 * fabric.</p>
 *
 * <p>In the happy path, a packet will come into this Connector from a particular {@link Plugin}, and handed
 * off to this switch to be routed to an appropriate {@link Plugin}. This will result in either an {@link
 * InterledgerFulfillPacket} or an {@link InterledgerRejectPacket}, which can then be returned back to the original
 * caller via the plugin that originated the {@link InterledgerPreparePacket}.</p>
 */
public interface IlpPacketSwitch {

  CompletableFuture<InterledgerFulfillPacket> sendData(
    final InterledgerAddress sourceAccountAddress, final InterledgerPreparePacket sourcePreparePacket
  ) throws InterledgerProtocolException;

  CompletableFuture<Void> sendMoney(final BigInteger amount) throws InterledgerProtocolException;

  boolean add(SendDataFilter sendDataFilter);

  void addFirst(SendDataFilter sendDataFilter);

  boolean add(SendMoneyFilter sendMoneyFilter);

  void addFirst(SendMoneyFilter sendMoneyFilter);

  SendDataFilterChain getSendDataFilterChain();

}
