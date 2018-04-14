package com.sappenin.ilpv4.plugins;

import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;

import java.math.BigInteger;
import java.util.concurrent.Future;

public class MockPlugin implements Plugin {

  @Override
  public void doConnect() {
    // NO OP
  }

  @Override
  public void doDisconnect() {
    // NO OP
  }

  @Override
  public Future<InterledgerFulfillPacket> sendPacket(InterledgerPreparePacket preparePacket) throws InterledgerProtocolException {
    return null;
  }

  @Override
  public void sendMoney(BigInteger amount) {
    // NO OP
  }

}