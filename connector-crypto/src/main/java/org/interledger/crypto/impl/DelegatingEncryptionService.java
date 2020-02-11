package org.interledger.crypto.impl;

import org.interledger.crypto.EncryptedSecret;
import org.interledger.crypto.EncryptionAlgorithm;
import org.interledger.crypto.EncryptionService;
import org.interledger.crypto.KeyMetadata;
import org.interledger.crypto.KeyStoreType;

import com.google.common.base.Preconditions;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementation that routes and delegates operations to an underlying set of EncryptionService, based on the
 * KeyStoreType of the operation. This allows for having multiple implementations (currently GCP and JKS) available
 * so that if a connector is being migrated from JKS to GCP (or vice versa) then the connector can still decrypt
 * old values encrypted with JKS and reencrypt them to GCP.
 */
public class DelegatingEncryptionService implements EncryptionService {

  private final Map<KeyStoreType, EncryptionService> serviceMap;

  public DelegatingEncryptionService(Set<EncryptionService> encryptionServices) {
    Preconditions.checkArgument(!encryptionServices.isEmpty(),
      "at least 1 encryption service must be configured");
    serviceMap = encryptionServices.stream().collect(
      Collectors.toMap(service -> service.keyStoreType(), service -> service)
    );
  }

  @Override
  public KeyStoreType keyStoreType() {
    return serviceMap.values().stream().map(EncryptionService::keyStoreType).findFirst().get();
  }

  @Override
  public byte[] decrypt(KeyMetadata keyMetadata, EncryptionAlgorithm encryptionAlgorithm, byte[] cipherMessage) {
    return getDelegate(keyMetadata).decrypt(keyMetadata, encryptionAlgorithm, cipherMessage);
  }

  @Override
  public EncryptedSecret encrypt(KeyMetadata keyMetadata, EncryptionAlgorithm encryptionAlgorithm, byte[] plainText) {
    return getDelegate(keyMetadata).encrypt(keyMetadata, encryptionAlgorithm, plainText);
  }

  @Override
  public <T> T withDecrypted(EncryptedSecret encryptedSecret, Function<byte[], T> callable) {
    return getDelegate(encryptedSecret.keyMetadata()).withDecrypted(encryptedSecret, callable);
  }

  private EncryptionService getDelegate(KeyMetadata keyMetadata) {
    return Optional.ofNullable(serviceMap.get(KeyStoreType.fromKeystoreTypeId(keyMetadata.platformIdentifier())))
      .orElseThrow(() ->
        new RuntimeException("no EncryptionService found for keystoreTypeId " + keyMetadata.platformIdentifier()));
  }


}