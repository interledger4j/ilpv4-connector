package org.interledger.connector;

import static org.interledger.crypto.CryptoConfigConstants.INTERLEDGER_CONNECTOR_KEYSTORE_GCP_ENABLED;
import static org.interledger.crypto.CryptoConfigConstants.INTERLEDGER_CONNECTOR_KEYSTORE_JKS_ENABLED;

import org.interledger.connector.RuntimeProperties.Runtimes;
import org.interledger.connector.core.ConfigConstants;
import org.interledger.crypto.KeyStoreType;

import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Utility methods for interacting with and learning about the current server runtime.
 */
public class RuntimeUtils {

  private static final String GOOGLE_CLOUD_PROJECT = "GOOGLE_CLOUD_PROJECT";

  /**
   * Looks to see if the connector is running with the GCP profile enabled. In this profile, the Connector attempts to
   * utilize various cloud-services, such as KMS, Redis, and others.
   */
  public static boolean gcpProfileEnabled(final Environment environment) {
    Objects.requireNonNull(environment);

    // If GCP profile is enabled, add those secrets...
    return Arrays.stream(environment.getActiveProfiles())
      .anyMatch(Runtimes.GCP::equalsIgnoreCase);
  }

  /**
   * Retrieve the GCP project name from the environment.
   */
  public static Optional<String> getGoogleCloudProjectId() {
    return Optional.ofNullable(System.getenv().get(GOOGLE_CLOUD_PROJECT))
      .filter(projectId -> !projectId.isEmpty());
  }

  /**
   * Determine the currently configured keystore type that should be used by this connector to store secret and private
   * keys. If no keystore type is configured, this method will default to {@link KeyStoreType#JKS}.
   *
   * @param environment An {@link Environment}.
   *
   * @return A {@link KeyStoreType} for the configured environment.
   */
  public static KeyStoreType determineKeystoreType(Environment environment) {
    Objects.requireNonNull(environment);

    if (isGcpKmsEnabled(environment)) {
      return KeyStoreType.GCP;
    } else if (isJksKmsEnabled(environment)) {
      return KeyStoreType.JKS;
    } else {
      throw new RuntimeException(
        String.format("Unsupported Keystore Type. Please defined either `%s` or `%s`",
          INTERLEDGER_CONNECTOR_KEYSTORE_GCP_ENABLED, INTERLEDGER_CONNECTOR_KEYSTORE_JKS_ENABLED)
      );
    }
  }

  /**
   * Helper method to determine if the Connector is currently running in `Wallet` mode.
   *
   * @param env The current runtime {@link Environment}.
   *
   * @return {@code true} if the Connector is currently running in wallet-mode; {@code false} otherwise.
   */
  public static boolean walletModeEnabled(final Environment env) {
    Objects.requireNonNull(env);
    return Arrays.stream(env.getActiveProfiles())
      .anyMatch(profile -> profile.equalsIgnoreCase(ConfigConstants.WALLET_MODE));
  }

  /**
   * Helper method to determine if the Connector is currently running in `Packet-Switch` mode. A Connector runs in this
   * mode by default whenever it is not running in Wallet Mode.
   *
   * @param env The current runtime {@link Environment}.
   *
   * @return {@code true}if the Connector is currently running in packet-switch-mode; {@code @false} otherwise.
   */
  public static boolean packetSwitchModeEnabled(final Environment env) {
    Objects.requireNonNull(env);
    return !walletModeEnabled(env);
  }

  /**
   * Determine if the GCP KMS key-store is enabled.
   *
   * * @param environment An {@link Environment} to read properties from.
   *
   * @return {@code true} if Goole Cloud Platform KMS is enabled; {@code @false} otherwise.
   */
  private static boolean isGcpKmsEnabled(final Environment environment) {
    Objects.requireNonNull(environment);
    return Optional.ofNullable(environment.getProperty(INTERLEDGER_CONNECTOR_KEYSTORE_GCP_ENABLED))
      .map(val -> val.equalsIgnoreCase(Boolean.TRUE.toString()))
      .orElse(false);
  }

  /**
   * Determine if the JKS key-store is enabled.
   *
   * @param environment An {@link Environment} to read properties from.
   *
   * @return {@code true} if JKS KMS is enabled; {@code @false} otherwise.
   */
  private static boolean isJksKmsEnabled(final Environment environment) {
    Objects.requireNonNull(environment);
    return Optional.ofNullable(environment.getProperty(INTERLEDGER_CONNECTOR_KEYSTORE_JKS_ENABLED))
      .map(val -> val.equalsIgnoreCase(Boolean.TRUE.toString()))
      .orElse(false);
  }
}
