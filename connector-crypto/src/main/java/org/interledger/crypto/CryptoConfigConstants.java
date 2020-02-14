package org.interledger.crypto;

import static org.interledger.connector.core.ConfigConstants.DOT;
import static org.interledger.connector.core.ConfigConstants.ENABLED;
import static org.interledger.connector.core.ConfigConstants.INTERLEDGER__CONNECTOR;

/**
 * Constants related to configuration of Encryption/Decryption services operated by this Connector.
 */
public class CryptoConfigConstants {

  public static final String INTERLEDGER_CONNECTOR_KEYSTORE = INTERLEDGER__CONNECTOR + DOT + "keystore";

  public static final String INTERLEDGER_CONNECTOR_KEYSTORE_GCP = INTERLEDGER_CONNECTOR_KEYSTORE + ".gcpkms";
  public static final String INTERLEDGER_CONNECTOR_KEYSTORE_GCP_ENABLED = INTERLEDGER_CONNECTOR_KEYSTORE_GCP + DOT + ENABLED;

  public static final String INTERLEDGER_CONNECTOR_KEYSTORE_LOCATION_ID = INTERLEDGER_CONNECTOR_KEYSTORE_GCP + ".locationId";
  public static final String INTERLEDGER_CONNECTOR_KEYSTORE_KEYRING_ID = INTERLEDGER_CONNECTOR_KEYSTORE_GCP + ".keyringId";

  ///////////////
  // JKS
  ///////////////

  public static final String FILENAME = "filename";
  public static final String PASSWORD = "password";

  public static final String INTERLEDGER_CONNECTOR_KEYSTORE_JKS = INTERLEDGER_CONNECTOR_KEYSTORE + ".jks";
  public static final String INTERLEDGER_CONNECTOR_KEYSTORE_JKS_ENABLED = INTERLEDGER_CONNECTOR_KEYSTORE_JKS + DOT + ENABLED;
  public static final String INTERLEDGER_CONNECTOR_KEYSTORE_JKS_FILENAME = INTERLEDGER_CONNECTOR_KEYSTORE_JKS + DOT + FILENAME;
  public static final String INTERLEDGER_CONNECTOR_KEYSTORE_JKS_PASSWORD = INTERLEDGER_CONNECTOR_KEYSTORE_JKS + DOT + PASSWORD;
  public static final String INTERLEDGER_CONNECTOR_KEYSTORE_JKS_SECRET0_ALIAS =
    INTERLEDGER_CONNECTOR_KEYSTORE_JKS + ".secret0_alias";
  public static final String INTERLEDGER_CONNECTOR_KEYSTORE_JKS_SECRET0_PASSWORD =
    INTERLEDGER_CONNECTOR_KEYSTORE_JKS + ".secret0_password";

  public static final String INTERLEDGER_CONNECTOR_KEYSTORE_JKS_FILENAME_DEFAULT = "crypto/crypto.p12";
  public static final String INTERLEDGER_CONNECTOR_KEYSTORE_JKS_PASSWORD_DEFAULT = "password";
  public static final String INTERLEDGER_CONNECTOR_KEYSTORE_JKS_SECRET0_ALIAS_DEFAULT = "secret0";
  public static final String INTERLEDGER_CONNECTOR_KEYSTORE_JKS_SECRET0_PASSWORD_DEFAULT = "password";
}
