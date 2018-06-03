package com.sappenin.ilpv4.server.btp;

public class BtpSubProtocolHandler {

  public byte[] handleBinaryMessage(BtpSession session, byte[] data) {
    throw new RuntimeException("Unexpected content type. Expected binary message.");
  }

  public String handleTextMessage(BtpSession account, String data) {
    throw new RuntimeException("Unexpected content type. Expected text message.");
  }

  public String handleJsonMessage(BtpSession account, String data) {
    throw new RuntimeException("Unexpected content type. Expected JSON message.");
  }

}
