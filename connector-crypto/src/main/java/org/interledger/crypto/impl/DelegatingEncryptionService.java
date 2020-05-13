package org.interledger.crypto.impl;

import static java.lang.String.format;
import static org.interledger.crypto.CryptoConfigConstants.INTERLEDGER_CONNECTOR_KEYSTORE;

import org.interledger.crypto.EncryptedSecret;
import org.interledger.crypto.EncryptionAlgorithm;
import org.interledger.crypto.EncryptionService;
import org.interledger.crypto.KeyMetadata;
import org.interledger.crypto.KeyStoreType;

import com.google.common.base.Preconditions;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementation that routes and delegates operations to an underlying set of EncryptionService, based on the
 * KeyStoreType of the operation. This allows for having multiple implementations (currently GCP and JKS) available so
 * that if a connector is being migrated from JKS to GCP (or vice versa) then the connector can still decrypt old values
 * encrypted with JKS and reencrypt them to GCP.
 */
public class DelegatingEncryptionService implements EncryptionService {

  private final Map<KeyStoreType, EncryptionService> serviceMap;

  /**
   * Required-args Constructor.
   *
   * @param encryptionServices A {@link }
   */
  public DelegatingEncryptionService(final Set<EncryptionService> encryptionServices) {
    Objects.requireNonNull(encryptionServices);
    Preconditions.checkArgument(
      !encryptionServices.isEmpty(), format(
        "At least one EncryptionService must be configured by setting the environment variable `%s`. "
          + "Consider using one these values (%s) or start the connector in dev-mode instead by setting the "
          + "environment variable `%s` to include `%s`.",
        INTERLEDGER_CONNECTOR_KEYSTORE + ".primary",
        Arrays.stream(KeyStoreType.values()) // Display all valid options.
          .map(val -> format("`%s`", val.getKeystoreId()))
          .collect(Collectors.joining(" ")),
        "spring.profiles.active", "dev"
      )
    );
    serviceMap = encryptionServices.stream()
      .collect(Collectors.toMap(service -> service.keyStoreType(), service -> service));
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
