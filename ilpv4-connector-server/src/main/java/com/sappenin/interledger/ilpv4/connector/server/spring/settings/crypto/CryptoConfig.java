package com.sappenin.interledger.ilpv4.connector.server.spring.settings.crypto;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(
  {
    GcpCryptoConfig.class,
    JksCryptoConfig.class
  }
)
public class CryptoConfig {
}
