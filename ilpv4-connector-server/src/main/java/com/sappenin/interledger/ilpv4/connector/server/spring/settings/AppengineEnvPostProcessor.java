package com.sappenin.interledger.ilpv4.connector.server.spring.settings;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.sappenin.interledger.ilpv4.connector.RuntimeUtils;
import org.interledger.secrets.gcp.GcpEncodedSecret;
import org.interledger.secrets.gcp.GcpSecretLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static org.interledger.secrets.gcp.GcpEncodedSecret.ENCODING_PREFIX;

/**
 * An implementation of {@link EnvironmentPostProcessor} that loops through all existing properties and looks for any
 * encrypted values. All of these values are decrypted, and then placed into a new property source that supersedes any
 * other sources so that systems can have access to protected values.
 *
 * @see "https://docs.spring.io/spring-boot/docs/current/reference/html/howto-spring-boot-application.html
 * #howto-customize-the-environment-or-application-context"
 */
public class AppengineEnvPostProcessor implements EnvironmentPostProcessor {

  // Application/ProjectId
  private static final String GOOGLE_CLOUD_PROJECT = "GOOGLE_CLOUD_PROJECT";
  private static final String GCP_KMS_LOCATION_ID = "gcp.kms.projectId";
  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final Optional<GcpSecretLoader> gcpSecretLoader;

  /**
   * Required-args Constructor.
   */
  public AppengineEnvPostProcessor() {
    if (RuntimeUtils.runningInGcp()) {
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
      this.gcpSecretLoader = Optional.of(new GcpSecretLoader(projectId, locationId));
    } else {
      this.gcpSecretLoader = Optional.empty();
    }
  }

  @VisibleForTesting
  AppengineEnvPostProcessor(final GcpSecretLoader gcpSecretLoader) {
    this.gcpSecretLoader = Optional.of(gcpSecretLoader);
  }

  @Override
  public void postProcessEnvironment(
    ConfigurableEnvironment environment, SpringApplication application
  ) {

    // If GAE profile is enabled, add those secrets...
    if (RuntimeUtils.runningInGcp() && RuntimeUtils.gcpProfileEnabled(environment)) {
      logger.info("Interleaving secrets from GCP KMS...");
      final PropertySource plainTextSecrets = this.loadSecretsFromGoogleKms(environment);
      environment.getPropertySources().addFirst(plainTextSecrets);
    }
  }

  /**
   * For any properties that begin with the prefix "gcpkms", attempt to decrypt them using the specified key meta-data.
   */
  @VisibleForTesting
  protected PropertySource loadSecretsFromGoogleKms(ConfigurableEnvironment environment) {
    final Map<String, Object> secrets = Maps.newHashMap();

    final GcpSecretLoader secretsLoader = gcpSecretLoader
      .orElseThrow(() -> new RuntimeException("GCPSecretsLoader required but not found!"));

    final MutablePropertySources propSrcs = environment.getPropertySources();
    StreamSupport.stream(propSrcs.spliterator(), false)
      .filter(propertySource -> propertySource instanceof EnumerablePropertySource)
      .map(propertySource -> ((EnumerablePropertySource) propertySource).getPropertyNames())
      .flatMap(Arrays::stream)
      .filter(propName -> propName.startsWith(ENCODING_PREFIX))
      .forEach(propName -> {
        final GcpEncodedSecret gcpEncodedSecret =
          GcpEncodedSecret.builder().encodedValue(environment.getProperty(propName)).build();

        // decrypt secret and place the decrypted value into the `secrets` property source.
        secrets.put(propName, secretsLoader.loadSecretFromGoogleKms(gcpEncodedSecret));
      });

    final PropertySource<?> secretPropertySource = new MapPropertySource("gcpSecrets", secrets);
    return secretPropertySource;
  }
}
