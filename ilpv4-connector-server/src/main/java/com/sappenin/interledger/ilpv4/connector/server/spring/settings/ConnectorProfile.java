package com.sappenin.interledger.ilpv4.connector.server.spring.settings;

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
public interface ConnectorProfile {
  // Developer modes...
  String DEV = "dev";
  String TEST = "test";
  String PROD = "prod";

  // System modes...

  /**
   * Only operates a single account using a single client plugin. The system must be configured with a `parent` account
   * that can be used to obtain an ILP Account address using ILDCP (or one must be statically configured in a
   * property).
   */
  String SINGLE_ACCOUNT_MODE = "single-mode";

  /**
   * Operates as a fully-functional connector, supporting routing, packet-switching, etc.
   */
  String CONNECTOR_MODE = "connector-mode";
}
