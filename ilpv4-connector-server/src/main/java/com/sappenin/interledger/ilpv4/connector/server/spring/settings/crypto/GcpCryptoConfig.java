package com.sappenin.interledger.ilpv4.connector.server.spring.settings.crypto;

import org.interledger.crypto.EncryptionService;
import org.interledger.crypto.impl.GcpEncryptionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.interledger.crypto.CryptoConfigConstants.ILPV4_CONNECTOR_KEYSTORE_JKS;
import static org.interledger.crypto.CryptoConfigConstants.ILPV4_CONNECTOR_KEYSTORE_JKS_FILENAME;
import static org.interledger.crypto.CryptoConfigConstants.ILPV4_CONNECTOR_KEYSTORE_LOCATION_ID;
import static org.interledger.crypto.CryptoConfigConstants.ILPV4_CONNECTOR_KEYSTORE_PROJECT_ID;

@Configuration
@ConditionalOnProperty(prefix = ILPV4_CONNECTOR_KEYSTORE_JKS, name = ILPV4_CONNECTOR_KEYSTORE_JKS_FILENAME)
public class GcpCryptoConfig {

  @Value("${" + ILPV4_CONNECTOR_KEYSTORE_PROJECT_ID + "}")
  private String gcpProjectId;

  @Value("${" + ILPV4_CONNECTOR_KEYSTORE_LOCATION_ID + "}")
  private String gcpLocationId;

  @Bean
  EncryptionService encryptionService() {
    return new GcpEncryptionService(gcpProjectId, gcpProjectId);
  }

}
