package org.interledger.connector.server.spring.settings.crypto;

import static org.interledger.connector.core.ConfigConstants.ENABLED;
import static org.interledger.connector.core.ConfigConstants.TRUE;
import static org.interledger.crypto.CryptoConfigConstants.INTERLEDGER_CONNECTOR_KEYSTORE_JKS;
import static org.interledger.crypto.CryptoConfigConstants.INTERLEDGER_CONNECTOR_KEYSTORE_JKS_FILENAME;
import static org.interledger.crypto.CryptoConfigConstants.INTERLEDGER_CONNECTOR_KEYSTORE_JKS_PASSWORD;
import static org.interledger.crypto.CryptoConfigConstants.INTERLEDGER_CONNECTOR_KEYSTORE_JKS_SECRET0_ALIAS;
import static org.interledger.crypto.CryptoConfigConstants.INTERLEDGER_CONNECTOR_KEYSTORE_JKS_SECRET0_PASSWORD;

import org.interledger.connector.crypto.ConnectorEncryptionService;
import org.interledger.connector.crypto.DefaultConnectorEncryptionService;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.crypto.EncryptionAlgorithm;
import org.interledger.crypto.EncryptionException;
import org.interledger.crypto.EncryptionService;
import org.interledger.crypto.JavaKeystoreLoader;
import org.interledger.crypto.KeyStoreType;
import org.interledger.crypto.impl.JksEncryptionService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.util.function.Supplier;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;

/**
 * Keystore Configuration that defines keys and secrets as being stored in a Java Keystore located on the classpath.
 */
@Configuration
@ConditionalOnProperty(prefix = INTERLEDGER_CONNECTOR_KEYSTORE_JKS, name = ENABLED, havingValue = TRUE)
public class JksCryptoConfig {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Value("${" + INTERLEDGER_CONNECTOR_KEYSTORE_JKS_FILENAME + "}")
  private String jksFilename;

  @Value("${" + INTERLEDGER_CONNECTOR_KEYSTORE_JKS_PASSWORD + "}")
  private String jksPassword;

  @Value("${" + INTERLEDGER_CONNECTOR_KEYSTORE_JKS_SECRET0_ALIAS + "}")
  private String secret0Alias;

  @Value("${" + INTERLEDGER_CONNECTOR_KEYSTORE_JKS_SECRET0_PASSWORD + "}")
  private String secret0Password;

  /**
   * Load the symmetric encryption SecretKey from a JKS on the classpath.
   */
  @Bean
  SecretKey secret0Key() throws Exception {
    final KeyStore ks = JavaKeystoreLoader.loadFromClasspath(jksFilename, jksPassword.toCharArray());

    // Password-protected keys are not yet supported
    if (ks.isKeyEntry(secret0Alias)) {
      final SecretKey secret0 = (SecretKey) ks.getKey(secret0Alias, secret0Password.toCharArray());
      return secret0;
    } else {
      throw new EncryptionException("No KeyEntry found for secret0Alias: " + secret0Alias);
    }
  }

  @Bean
  JksEncryptionService jksEncryptionService(SecretKey secret0Key) throws NoSuchAlgorithmException {
    return new JksEncryptionService(secret0Key);
  }

  @Bean
  ConnectorEncryptionService jksConnectorEncryptionService(
    EncryptionService encryptionService, Supplier<ConnectorSettings> connectorSettings
  ) {
    return new DefaultConnectorEncryptionService(encryptionService,
      KeyStoreType.JKS,
      jksFilename,
      connectorSettings.get().keys(),
      EncryptionAlgorithm.AES_GCM);
  }

  @PostConstruct
  public void init() {
    // Log a warning if the default JKS filename and/or password is being used.
    if ("crypto/crypto.p12".equals(jksFilename)) {
      logger.warn("\n###################\n"
        + "WARNING: Connector is using the DEV-MODE Java Keystore (JKS). The keys in this file are publicly known and "
        + "should not be used in a production environment. Consider using a different JKS file or use a KMS. Consult "
        + "the documentation for more details."
        + "\n###################"
      );
    }
  }
}
