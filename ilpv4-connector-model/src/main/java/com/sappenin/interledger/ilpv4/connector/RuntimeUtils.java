package com.sappenin.interledger.ilpv4.connector;

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

    // If GAE profile is enabled, add those secrets...
    final boolean runningInGcp = Arrays.stream(environment.getActiveProfiles())
      .filter(profile -> ConnectorProfiles.GCP.equals(profile))
      .findAny().map($ -> true).orElse(false);
    return runningInGcp;
  }

  /**
   * Determines if the runtime is running in Google Cloud Platform (GCP) by looking to see if the environment property
   * `GOOGLE_CLOUD_PROJECT` is present. If this is found, it means we're running in GCP (or we're running in an
   * environment where the operator wants the runtime to assume this is the case).
   *
   * @return {@code true} if the runtime is running in GCP; {@code false} otherwise.
   */
  public static boolean runningInGcp() {
    // If GAE profile is enabled, add those secrets...
    final boolean runningInGcp = getGoogleCloudProjectId().isPresent();
    return runningInGcp;
  }

  /**
   * Retrieve the GCP project name from the environment.
   */
  public static Optional<String> getGoogleCloudProjectId() {
    return Optional.ofNullable(System.getenv().get(GOOGLE_CLOUD_PROJECT))
      .filter(projectId -> !projectId.isEmpty());
  }

}
