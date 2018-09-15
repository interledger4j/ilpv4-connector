package com.sappenin.ilpv4.plugins.btp;

public abstract class BtpSubProtocolHandler {

  public abstract byte[] handleBinaryMessage(BtpSession session, byte[] data);

  public String handleTextMessage(BtpSession account, String data) {
    throw new RuntimeException("Text data handling is not yet implemented!");
  }

  public String handleJsonMessage(BtpSession account, String data) {
    throw new RuntimeException("JSON data handling is not yet implemented!");
  }

}
