package com.sappenin.interledger.ilpv4.connector.server.spring.settings;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.sappenin.interledger.ilpv4.connector.RuntimeUtils;
import org.interledger.crypto.EncryptedSecret;
import org.interledger.crypto.EncryptionException;
import org.interledger.crypto.EncryptionService;
import org.interledger.crypto.KeyStoreType;
import org.interledger.crypto.JavaKeystoreLoader;
import org.interledger.crypto.impl.GcpEncryptionService;
import org.interledger.crypto.impl.JksEncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import javax.crypto.SecretKey;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static org.interledger.crypto.CryptoConfigConstants.ILPV4_CONNECTOR_KEYSTORE_JKS_FILENAME;
import static org.interledger.crypto.CryptoConfigConstants.ILPV4_CONNECTOR_KEYSTORE_JKS_FILENAME_DEFAULT;
import static org.interledger.crypto.CryptoConfigConstants.ILPV4_CONNECTOR_KEYSTORE_JKS_PASSWORD;
import static org.interledger.crypto.CryptoConfigConstants.ILPV4_CONNECTOR_KEYSTORE_JKS_PASSWORD_DEFAULT;
import static org.interledger.crypto.CryptoConfigConstants.ILPV4_CONNECTOR_KEYSTORE_JKS_SECRET0_ALIAS;
import static org.interledger.crypto.CryptoConfigConstants.ILPV4_CONNECTOR_KEYSTORE_JKS_SECRET0_ALIAS_DEFAULT;
import static org.interledger.crypto.CryptoConfigConstants.ILPV4_CONNECTOR_KEYSTORE_JKS_SECRET0_PASSWORD;
import static org.interledger.crypto.CryptoConfigConstants.ILPV4_CONNECTOR_KEYSTORE_JKS_SECRET0_PASSWORD_DEFAULT;
import static org.interledger.crypto.EncryptedSecret.ENCODING_PREFIX;

/**
 * An implementation of {@link EnvironmentPostProcessor} that loops through all existing properties and looks for any
 * encrypted values. All of these values are decrypted, and then placed into a new property source that supersedes any
 * other sources so that systems can have access to protected values in unencrypted form.
 *
 * @see "https://docs.spring.io/spring-boot/docs/current/reference/html/howto-spring-boot-application.html
 * #howto-customize-the-environment-or-application-context"
 */
public class AppengineEnvPostProcessor implements EnvironmentPostProcessor {

  // Application/ProjectId
  private static final String GOOGLE_CLOUD_PROJECT = "GOOGLE_CLOUD_PROJECT";
  private static final String GCP_KMS_LOCATION_ID = "gcp.kms.projectId";

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private Optional<EncryptionService> encryptionService;

  public AppengineEnvPostProcessor() {
  }

  @VisibleForTesting
  AppengineEnvPostProcessor(final EncryptionService encryptionService) {
    this.encryptionService = Optional.of(encryptionService);
  }

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {

    final KeyStoreType keyStoreType = RuntimeUtils.determineKeystoreType(environment);

    // Construct proper encryption service.
    if (keyStoreType.equals(KeyStoreType.GCP)) {
      this.encryptionService = Optional.of(this.constructGcpEncryptionService());
    } else {
      try {
        this.encryptionService = Optional.of(this.constructJksEncryptionService(environment));
      } catch (Exception e) {
        throw new RuntimeException(e.getMessage(), e);
      }
    }

    logger.info("Interleaving secrets into the Environment using EncryptionService...");
    final PropertySource plainTextSecrets = this.interleaveSecretsFromEncryptionService(environment);
    environment.getPropertySources().addFirst(plainTextSecrets);
  }

  private EncryptionService constructGcpEncryptionService() {
    final String projectId = RuntimeUtils.getGoogleCloudProjectId()
      .orElseThrow(() -> new RuntimeException(
        String.format("System property `%s` was required by not specified.", GOOGLE_CLOUD_PROJECT))
      );
    final String locationId = Optional.ofNullable(System.getenv().get(GCP_KMS_LOCATION_ID))
      .orElseGet(() -> {
        logger.warn(String.format("System property `%s` was required by not specified. Defaulting to `global`",
          GOOGLE_CLOUD_PROJECT));
        return "global";
      });
    return new GcpEncryptionService(projectId, locationId);
  }

  private EncryptionService constructJksEncryptionService(Environment environment) throws Exception {
    return new JksEncryptionService(loadSecret0FromJks(environment));
  }

  private SecretKey loadSecret0FromJks(Environment environment) throws Exception {
    final String jksFilename = environment.getProperty(
      ILPV4_CONNECTOR_KEYSTORE_JKS_FILENAME, ILPV4_CONNECTOR_KEYSTORE_JKS_FILENAME_DEFAULT
    );
    final char[] jksPassword = environment.getProperty(
      ILPV4_CONNECTOR_KEYSTORE_JKS_PASSWORD, ILPV4_CONNECTOR_KEYSTORE_JKS_PASSWORD_DEFAULT
    ).toCharArray();
    final String secret0Alias = environment.getProperty(
      ILPV4_CONNECTOR_KEYSTORE_JKS_SECRET0_ALIAS, ILPV4_CONNECTOR_KEYSTORE_JKS_SECRET0_ALIAS_DEFAULT);
    final char[] secret0Password = environment.getProperty(
      ILPV4_CONNECTOR_KEYSTORE_JKS_SECRET0_PASSWORD, ILPV4_CONNECTOR_KEYSTORE_JKS_SECRET0_PASSWORD_DEFAULT
    ).toCharArray();

    final KeyStore keystore = JavaKeystoreLoader.loadFromClasspath(jksFilename, jksPassword);

    // Password-protected keys are not yet supported
    if (keystore.isKeyEntry(secret0Alias)) {
      final SecretKey secret0 = (SecretKey) keystore.getKey(secret0Alias, secret0Password);
      return secret0;
    } else {
      throw new EncryptionException("No KeyEntry found for secret0Alias: " + secret0Alias);
    }
  }

  /**
   * For any properties that begin with the prefix "gcpkms", attempt to decrypt them using the specified key meta-data.
   */
  @VisibleForTesting
  protected PropertySource interleaveSecretsFromEncryptionService(ConfigurableEnvironment environment) {
    final Map<String, Object> secrets = Maps.newHashMap();

    if (this.encryptionService == null) {
      throw new RuntimeException("EncryptionService required but not found!");
    }

    final MutablePropertySources propSrcs = environment.getPropertySources();
    StreamSupport.stream(propSrcs.spliterator(), false)
      .filter(propertySource -> propertySource instanceof EnumerablePropertySource)
      .map(propertySource -> ((EnumerablePropertySource) propertySource).getPropertyNames())
      .flatMap(Arrays::stream)
      // Encrypted properties will have a value starting with `enc:`
      .filter(propName -> environment.getProperty(propName).startsWith(ENCODING_PREFIX))
      .forEach(propName -> {
        final EncryptedSecret encryptedSecret = EncryptedSecret.fromEncodedValue(environment.getProperty(propName));
        // decrypt secret and place the decrypted value into the `secrets` property source.
        secrets.put(propName, new String(encryptionService.get().decrypt(encryptedSecret)));
      });

    final PropertySource<?> secretPropertySource = new MapPropertySource("gcpSecrets", secrets);
    return secretPropertySource;
  }
}
