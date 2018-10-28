package com.sappenin.ilpv4.plugins.btp;

import org.interledger.btp.*;

public abstract class BtpPacketHandler<T> {

  public final T handle(final BtpPacket btpPacket) {

    switch (btpPacket.getType()) {
      case MESSAGE: {
        return this.handleBtpMessage((BtpMessage) btpPacket);
      }
      case ERROR: {
        return this.handleBtpError((BtpError) btpPacket);
      }
      case RESPONSE: {
        return this.handleBtpResponse((BtpResponse) btpPacket);
      }
      case TRANSFER: {
        return this.handleBtpTransfer((BtpTransfer) btpPacket);
      }
      default: {
        throw new RuntimeException(String.format("Unsupported BtpPacket Type: %s", btpPacket.getType()));
      }
    }
  }

  protected abstract T handleBtpMessage(final BtpMessage btpMessage);

  protected abstract T handleBtpTransfer(final BtpTransfer btpTransfer);

  protected abstract T handleBtpError(final BtpError btpError);

  protected abstract T handleBtpResponse(final BtpResponse btpResponse);

}
