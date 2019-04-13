package com.sappenin.interledger.ilpv4.connector;

/**
 * <p>Profiles can be activated in a variety of ways:</p>
 *
 * <pre>
 *   <ul>
 *     <li>Set profile via JVM system propery: -Dspring.profiles.active=server</li>
 *     <li>Via Spring Environment: env.setActiveProfiles("someProfile");</li>
 *     <li>Via OS Environment: `export spring_profiles_active=dev`</li>
 *     <li>Via Maven: </li>
 *   </ul>
 * </pre>
 */
public interface ConnectorProfiles {
  // Developer modes...
  String DEV = "dev";
  String TEST = "test";
  String PROD = "prod";

  /**
   * Add this profile if running in GCP.
   */
  String GCP = "gcp";
}
