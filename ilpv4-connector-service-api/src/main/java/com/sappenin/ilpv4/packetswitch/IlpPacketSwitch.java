package com.sappenin.ilpv4.packetswitch;

import com.sappenin.ilpv4.packetswitch.filters.SendDataFilter;
import com.sappenin.ilpv4.packetswitch.filters.SendDataFilterChain;
import com.sappenin.ilpv4.packetswitch.filters.SendMoneyFilter;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;
import org.interledger.plugin.lpiv2.Plugin;

import java.math.BigInteger;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * <p>A collection of PacketHandlers that is applied to every Packet that traverses the Connector's routing
 * packetswitch. A packet will come into the connector from a particular {@link Plugin}, potentially be routed to
 * another {@link Plugin}, which will result in a {@link InterledgerFulfillPacket}, which can then be returned back to
 * the original caller.</p>
 */
public interface IlpPacketSwitch {

  CompletableFuture<InterledgerFulfillPacket> sendData(
    final InterledgerAddress sourceAccountAddress, final InterledgerPreparePacket sourcePreparePacket
  ) throws InterledgerProtocolException;

  CompletableFuture<InterledgerFulfillPacket> sendMoney(final BigInteger amount) throws InterledgerProtocolException;

  boolean add(SendDataFilter sendDataFilter);

  void addFirst(SendDataFilter sendDataFilter);

  boolean add(SendMoneyFilter sendMoneyFilter);

  void addFirst(SendMoneyFilter sendMoneyFilter);

  SendDataFilterChain getSendDataFilterChain();

}
