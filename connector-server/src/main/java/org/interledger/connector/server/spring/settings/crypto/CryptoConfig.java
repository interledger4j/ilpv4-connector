package org.interledger.connector.server.spring.settings.crypto;

import static org.interledger.crypto.CryptoConfigConstants.INTERLEDGER_CONNECTOR_KEYSTORE;

import org.interledger.connector.crypto.ConnectorEncryptionService;
import org.interledger.crypto.EncryptionService;
import org.interledger.crypto.KeyStoreType;
import org.interledger.crypto.impl.DelegatingEncryptionService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@Configuration
@Import(
  {
    GcpCryptoConfig.class,
    JksCryptoConfig.class
  }
)
public class CryptoConfig {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  /**
   * If multiple encryption keystores are enabled, configures which keystore is the primary one to be used for
   * encrypting new values. Default value is the empty string so that a vali error message is emitted.
   */
  @Value("${" + INTERLEDGER_CONNECTOR_KEYSTORE + ".primary:}")
  private String primaryKeystore;

  @Bean
  @Primary
  public EncryptionService encryptionService(Set<EncryptionService> encryptionServices) {
    return new DelegatingEncryptionService(encryptionServices);
  }

  @Bean
  @Primary
  public ConnectorEncryptionService connectorEncryptionService(
    List<ConnectorEncryptionService> connectorEncryptionServices
  ) {
    KeyStoreType primaryKeyStoreType = KeyStoreType.fromKeystoreTypeId(primaryKeystore);
    Objects.requireNonNull(primaryKeyStoreType);
    return connectorEncryptionServices.stream()
      .filter(connectorEncryptionService -> connectorEncryptionService.getKeyStoreType().equals(primaryKeyStoreType))
      .findFirst()
      .orElseThrow(
        () -> new IllegalStateException("No ConnectorEncryptionService found for primaryKeystore " + primaryKeystore));
  }

}
