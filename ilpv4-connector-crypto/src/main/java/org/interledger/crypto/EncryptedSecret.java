package org.interledger.crypto;

import org.immutables.value.Value;

import java.util.Base64;
import java.util.Objects;

/**
 * Represents an encrypted secret that is encoded into a particular String format that this Connector supports. See
 * {@link EncryptedSecret#encodedValue()} for more details.
 */
public interface EncryptedSecret {

  String ENCODING_PREFIX = "enc";

  static ImmutableEncryptedSecret.Builder builder() {
    return ImmutableEncryptedSecret.builder();
  }

  /**
   * Construct a {@link EncryptedSecret} from {@code encodedValue} that conforms to the format defined in {@link
   * EncryptedSecret#encodedValue()}.
   *
   * @param encodedValue A {@link String} that conforms to the format defined in {@link EncryptedSecret#encodedValue()}.
   *
   * @return
   */
  static EncryptedSecret fromEncodedValue(final String encodedValue) {
    Objects.requireNonNull(encodedValue);

    if (!encodedValue.startsWith("enc:")) {
      throw new IllegalArgumentException(
        "encodedValue must start with `enc` prefix. Value was instead: " + encodedValue);
    }

    final String[] encodedValues = encodedValue.split(":");

    final ImmutableKeyMetadata.Builder keyMetadataBuilder = ImmutableKeyMetadata.builder();
    try {
      keyMetadataBuilder.platformIdentifier(encodedValues[1].toLowerCase());
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalStateException("Invalid platformIdentifier!", e);
    }
    try {
      keyMetadataBuilder.keyringIdentifier(encodedValues[2].toLowerCase());
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalStateException("Invalid keyringIdentifier!", e);
    }

    try {
      keyMetadataBuilder.keyIdentifier(encodedValues[3]);
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalStateException("Invalid keyIdentifier!", e);
    }

    try {
      keyMetadataBuilder.keyVersion(encodedValues[4].toLowerCase());
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalStateException("Invalid keyVersion!", e);
    }

    final EncryptionAlgorithm encryptionAlgorithm;
    try {
      encryptionAlgorithm = EncryptionAlgorithm.fromEncodedValue(encodedValues[5].toLowerCase());
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalStateException("Invalid encryptionAlgorithm!", e);
    }

    final String cipherText;
    try {
      cipherText = encodedValue.split(":")[6];
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalStateException("Invalid ciphertext!", e);
    }

    return builder()
      .keyMetadata(keyMetadataBuilder.build())
      .encryptionAlgorithm(encryptionAlgorithm)
      .cipherMessage(Base64.getUrlDecoder().decode(cipherText))
      .build();
  }

  /**
   * Metadata about the key used to encrypt this secret.
   *
   * @return A {@link KeyMetadata}.
   */
  KeyMetadata keyMetadata();

  /**
   * The algorithm used to encrypt this secret.
   *
   * @return A {@link EncryptionAlgorithm}.
   */
  EncryptionAlgorithm encryptionAlgorithm();

  /**
   * The actual encrypted cipher-message as a byte-array. This byte array is a "message" as opposed to the actual
   * cipherText because it likely has padding to support AES/GCM mode.
   *
   * @return A {@link String} that conforms to the format defined in {@link EncryptedSecret#encodedValue()}.
   */
  byte[] cipherMessage();

  /**
   * The encoded secret as a {@link String} composed of the following components:
   *
   * <pre>
   * <ol>
   *   <li><b>Encoding Scheme Identifier:</b>By default, `enc`.</li>
   *   <li><b>Provider Identifier:</b> The identifier of the underlying provider that can decode this encrypted
   *   value.</li>
   *   <li><b>Algorithm Identifier:</b> The encryption algorithm identifier per {@link EncryptionAlgorithm}.</li>
   *   <li><b>Keyring Identifier:</b> The identifier of the keyring that holds the private-key used to decrypt this
   *   value. For example, `foo_key_ring`</li>
   *   <li><b>Encryption Key Identifier:</b> The identifier of the private-key used to encrypt this value. For
   *   example, `db_password`.</li>
   *   <li><b>Encryption Key Version:</b> The version of the private-key used to encrypt this value. For example, `1`
   *   .</li>
   *   <li><b>Ciphertext:</b> The encrypted ciphertext for the secret value, encoded as a Base64Url string with padding.</li>
   * </ol>
   * </pre>
   *
   * An example of an encrypted secret encoded using this scheme is: `enc:gcpkms:kr1:GSK:db_password:2:VGhpcyBpcyBhIHRoZSBzZWNyZXQ=`
   *
   * @return A {@link String} containing an encrypted secret encoded using this scheme.
   */
  String encodedValue();

  @Value.Immutable
  abstract class AbstractEncryptedSecret implements EncryptedSecret {

    @Override
    @Value.Derived
    public String encodedValue() {
      return String.format(
        "%s:%s:%s:%s:%s:%s:%s",
        ENCODING_PREFIX,
        keyMetadata().platformIdentifier(),
        keyMetadata().keyringIdentifier(),
        keyMetadata().keyIdentifier(),
        keyMetadata().keyVersion(),
        encryptionAlgorithm().getAlgorithm(),
        Base64.getUrlEncoder().encodeToString(cipherMessage())
      );
    }

    @Override
    public String toString() {
      return this.encodedValue();
    }
  }

}
