package com.sappenin.ilpv4.server.btp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Spring configuration for BTP over Websocket.
 */
@Configuration
@EnableWebSocket
public class BtpWebSocketConfig implements WebSocketConfigurer {

  @Autowired
  BtpSocketHandler btpSocketHandler;

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(btpSocketHandler, "/");
  }

  @Bean
  BtpSocketHandler btpSocketHandler(final BtpSubProtocolHandlerRegistry btpSubProtocolHandlerRegistry) {
    return new BtpSocketHandler(btpSubProtocolHandlerRegistry);
  }

  @Bean
  BtpSubProtocolHandlerRegistry btpSubProtocolHandlerRegistry() {
    BtpSubProtocolHandlerRegistry registry = new BtpSubProtocolHandlerRegistry();
    registry.putHandler("ilp", new MockIlpHandler());
    return registry;
  }

  class MockIlpHandler extends BtpSubProtocolHandler {

    @Override
    public byte[] handleBinaryMessage(BtpSession session, byte[] data) {
      System.out.println("ILP Packet from " + session.getAccountId());

      //
      // return IlpConnector.procesPayment(packet)
      return new byte[]{};
    }
  }

}