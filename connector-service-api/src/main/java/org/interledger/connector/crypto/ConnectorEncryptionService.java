package org.interledger.connector.crypto;

import org.interledger.crypto.Decryptor;
import org.interledger.crypto.EncryptedSecret;

public interface ConnectorEncryptionService {

  EncryptedSecret encryptWithAccountSettingsKey(byte[] secret);

  EncryptedSecret encryptWithSecret0(byte[] secret);

  Decryptor getDecryptor();

}
