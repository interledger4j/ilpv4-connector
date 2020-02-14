package org.interledger.connector.server.spring.settings.crypto;

import static org.interledger.connector.core.ConfigConstants.ENABLED;
import static org.interledger.connector.core.ConfigConstants.TRUE;
import static org.interledger.crypto.CryptoConfigConstants.INTERLEDGER_CONNECTOR_KEYSTORE_GCP;
import static org.interledger.crypto.CryptoConfigConstants.INTERLEDGER_CONNECTOR_KEYSTORE_KEYRING_ID;
import static org.interledger.crypto.CryptoConfigConstants.INTERLEDGER_CONNECTOR_KEYSTORE_LOCATION_ID;

import org.interledger.connector.crypto.ConnectorEncryptionService;
import org.interledger.connector.crypto.DefaultConnectorEncryptionService;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.crypto.EncryptionAlgorithm;
import org.interledger.crypto.EncryptionService;
import org.interledger.crypto.KeyStoreType;
import org.interledger.crypto.impl.GcpEncryptionService;

import com.google.api.gax.core.CredentialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Supplier;

@Configuration
@ConditionalOnProperty(prefix = INTERLEDGER_CONNECTOR_KEYSTORE_GCP, name = ENABLED, havingValue = TRUE)
public class GcpCryptoConfig {

  @Value("${" + INTERLEDGER_CONNECTOR_KEYSTORE_LOCATION_ID + "}")
  private String gcpLocationId;

  @Value("${" + INTERLEDGER_CONNECTOR_KEYSTORE_KEYRING_ID + "}")
  private String gcpKeyringId;

  @Bean
  GcpEncryptionService gcpEncryptionService(GcpProjectIdProvider gcpProjectIdProvider, CredentialsProvider credentialsProvider) {
    return new GcpEncryptionService(gcpProjectIdProvider.getProjectId(), gcpLocationId, credentialsProvider);
  }

  @Bean
  ConnectorEncryptionService gcpConnectorEncryptionService(EncryptionService encryptionService,
                                                        Supplier<ConnectorSettings> connectorSettings) {
    return new DefaultConnectorEncryptionService(encryptionService,
      KeyStoreType.GCP,
      gcpKeyringId,
      connectorSettings.get().keys(),
      EncryptionAlgorithm.GOOGLE_SYMMETRIC);
  }

}
