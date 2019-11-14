package org.interledger.connector.crypto;

import org.interledger.connector.settings.ConnectorKeys;
import org.interledger.crypto.Decryptor;
import org.interledger.crypto.EncryptedSecret;
import org.interledger.crypto.EncryptionAlgorithm;
import org.interledger.crypto.EncryptionService;
import org.interledger.crypto.ImmutableKeyMetadata;
import org.interledger.crypto.KeyMetadata;
import org.interledger.crypto.KeyStoreType;

public class DefaultConnectorEncryptionService implements ConnectorEncryptionService {

  private final EncryptionService encryptionService;
  private final KeyStoreType keyStoreType;
  private final String keyringIdentifier;
  private final ConnectorKeys connectorKeys;
  private final EncryptionAlgorithm encryptionAlgorithm;

  public DefaultConnectorEncryptionService(EncryptionService encryptionService,
                                           KeyStoreType keyStoreType,
                                           String keyringIdentifier,
                                           ConnectorKeys connectorKeys,
                                           EncryptionAlgorithm encryptionAlgorithm) {
    this.encryptionService = encryptionService;
    this.keyStoreType = keyStoreType;
    this.keyringIdentifier = keyringIdentifier;
    this.connectorKeys = connectorKeys;
    this.encryptionAlgorithm = encryptionAlgorithm;
  }

  @Override
  public EncryptedSecret encryptWithAccountSettingsKey(byte[] secret) {
    KeyMetadata keyMetadata = newKeyMetadataBuilder()
        .keyIdentifier(connectorKeys.accountSettings().alias())
        .keyVersion(connectorKeys.accountSettings().version())
        .build();

    return encryptionService.encrypt(keyMetadata, encryptionAlgorithm, secret);
  }

  @Override
  public EncryptedSecret encryptWithSecret0(byte[] secret) {
    KeyMetadata keyMetadata = newKeyMetadataBuilder()
        .keyIdentifier(connectorKeys.secret0().alias())
        .keyVersion(connectorKeys.secret0().version())
        .build();

    return encryptionService.encrypt(keyMetadata, encryptionAlgorithm, secret);
  }

  @Override
  public Decryptor getDecryptor() {
    return encryptionService;
  }

  private ImmutableKeyMetadata.Builder newKeyMetadataBuilder() {
    return KeyMetadata.builder()
        .keyringIdentifier(keyringIdentifier)
        .platformIdentifier(keyStoreType.getKeystoreId());
  }
}
