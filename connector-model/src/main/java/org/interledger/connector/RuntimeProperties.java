package org.interledger.connector;

import org.interledger.crypto.KeyStoreType;

public interface RuntimeProperties {

  String DB = "db";

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
  interface ConnectorProfiles {
    // Developer modes...
    String DEV = "dev";
    String TEST = "test";
    String PROD = "prod";
  }

  /**
   * Represents various runtime environments.
   */
  interface Runtimes {
    String DEFAULT = "";

    //Add this profile if running in Google Cloud Platform.
    String GCP = "gcp";
  }

  interface Keystores {
    String GCP_KMS = KeyStoreType.GCP.getKeystoreId();
    String JAVA_KEY_STORE = KeyStoreType.JKS.getKeystoreId();
    //String VAULT = KeyStoreType.VAULT.getKeystoreId();
  }

  interface Databases {
    String H2 = "h2";
    String REDIS = "redis";
  }

}
