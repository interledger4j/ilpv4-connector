package org.interledger.crypto;

import static org.interledger.crypto.CryptoConstants.KEYSTORE_ID_GCP_KMS;
import static org.interledger.crypto.CryptoConstants.KEYSTORE_ID_JKS;
import static org.interledger.crypto.CryptoConstants.KEYSTORE_ID_VAULT;

/**
 * Unique identifiers of the providers of underlying secrets-storage, which typically treat key-material as "secret
 * information" whereas the data encrypted by these secrets is generally not stored in these platforms. For example, a
 * database password would not be stored in the secrets-store. Instead, a private key or symmetric key would be stored
 * in the secrets store, and the encrypted database password (which is also _technically_ a secret value) would be
 * stored in some other system.
 */
public enum KeyStoreType {

  /**
   * Private keys and symmetric secrets are stored in a Java Keystore (JKS) located in the classpath.
   */
  JKS(KEYSTORE_ID_JKS),

  /**
   * Private keys and symmetric secrets are stored in Google Cloud Platform KMS.
   */
  GCP(KEYSTORE_ID_GCP_KMS),

  /**
   * Private keys and symmetric secrets are stored in Hashicorp's Vault
   */
  VAULT(KEYSTORE_ID_VAULT);

  private String keystoreId;

  KeyStoreType(final String keystoreId) {
    this.keystoreId = keystoreId;
  }

  public static KeyStoreType fromKeystoreTypeId(final String keystoreTypeId) {
    switch (keystoreTypeId.toLowerCase()) {
      case KEYSTORE_ID_JKS: {
        return JKS;
      }
      case KEYSTORE_ID_GCP_KMS: {
        return GCP;
      }
      case KEYSTORE_ID_VAULT: {
        return VAULT;
      }
      default: {
        throw new RuntimeException("Invalid KeyStoreType: " + keystoreTypeId);
      }
    }
  }

  public String getKeystoreId() {
    return this.keystoreId;
  }
}
