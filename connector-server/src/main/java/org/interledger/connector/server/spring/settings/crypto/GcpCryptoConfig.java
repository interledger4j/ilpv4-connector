package org.interledger.connector.server.spring.settings.crypto;

import static org.interledger.connector.core.ConfigConstants.ENABLED;
import static org.interledger.connector.core.ConfigConstants.TRUE;
import static org.interledger.crypto.CryptoConfigConstants.GOOGLE_CLOUD_PROJECT;
import static org.interledger.crypto.CryptoConfigConstants.INTERLEDGER_CONNECTOR_KEYSTORE_GCP;
import static org.interledger.crypto.CryptoConfigConstants.INTERLEDGER_CONNECTOR_KEYSTORE_LOCATION_ID;

import org.interledger.connector.crypto.ConnectorEncryptionService;
import org.interledger.connector.crypto.DefaultConnectorEncryptionService;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.crypto.EncryptionAlgorithm;
import org.interledger.crypto.EncryptionService;
import org.interledger.crypto.KeyStoreType;
import org.interledger.crypto.impl.GcpEncryptionService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Supplier;

@Configuration
@ConditionalOnProperty(prefix = INTERLEDGER_CONNECTOR_KEYSTORE_GCP, name = ENABLED, havingValue = TRUE)
public class GcpCryptoConfig {

  @Value("${" + GOOGLE_CLOUD_PROJECT + "}")
  private String gcpProjectId;

  @Value("${" + INTERLEDGER_CONNECTOR_KEYSTORE_LOCATION_ID + "}")
  private String gcpLocationId;

  @Bean
  EncryptionService encryptionService() {
    return new GcpEncryptionService(gcpProjectId, gcpLocationId);
  }

  @Bean
  ConnectorEncryptionService connectorEncryptionService(EncryptionService encryptionService,
                                                        Supplier<ConnectorSettings> connectorSettings) {
    return new DefaultConnectorEncryptionService(encryptionService,
      KeyStoreType.GCP,
      gcpLocationId,
      connectorSettings.get().keys(),
      EncryptionAlgorithm.GOOGLE_SYMMETRIC);
  }

}
