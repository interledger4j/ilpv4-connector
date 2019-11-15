package org.interledger.connector.crypto;

import org.interledger.crypto.Decryptor;
import org.interledger.crypto.EncryptedSecret;

/**
 * Encryption methods specific to a connector.
 */
public interface ConnectorEncryptionService {

  /**
   * Encrypts a secret using the account settings key
   * @param secret to encrypt
   * @return encrypted
   */
  EncryptedSecret encryptWithAccountSettingsKey(byte[] secret);

  /**
   * Encrypts a secret using the secret0 key
   * @param secret to encrypt
   * @return encrypted
   */
  EncryptedSecret encryptWithSecret0(byte[] secret);

  /**
   * Gets the default decryptor for the connector.
   * @return decryptor
   */
  Decryptor getDecryptor();

}
