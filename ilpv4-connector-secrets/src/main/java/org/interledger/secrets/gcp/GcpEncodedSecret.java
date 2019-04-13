package org.interledger.secrets.gcp;

import org.immutables.value.Value;

/**
 * Represents an encrypted secret that is encoded into a particular String format unique to GCP KMS.
 */
public interface GcpEncodedSecret {

  String ENCODING_PREFIX = "gcpkms";

  static ImmutableGcpEncodedSecret.Builder builder() {
    return ImmutableGcpEncodedSecret.builder();
  }

  /**
   * The unique identifier of the keyring that holds the the private-key used to encrypt this encoded secret.
   *
   * @return
   */
  String keyringIdentifier();

  /**
   * The encryption algorithm used to encrypt this value.
   *
   * @return
   */
  GcpEncryptionAlgorithm encryptionAlgorithm();

  /**
   * The unique identifier for the private-key used to encrypt this encoded secret.
   *
   * @return
   */
  String keyIdentifier();

  /**
   * The version of the encryption key used to encrypt this secret.
   *
   * @return
   */
  String keyVersion();

  /**
   * The actual encrypted cipher-text for this secret.
   *
   * @return
   */
  String cipherText();

  /**
   * The encoded secret as a {@link String} composed of the following components:
   *
   * <pre>
   * <ol>
   *   <li><b>Encoding Scheme Identifier:</b>By default, `enc`.</li>
   *   <li><b>Keyring Identifier:</b>The identifier of the keyring that holds the private-key used to decrypt this
   *   value. For example, `foo_key_ring`</li>
   *   <li><b>Algorithm Identifier:</b>The encryption algorithm identifier per {@link GcpEncryptionAlgorithm}.</li>
   *   <li><b>Encryption Key Identifier:</b>The identifier of the private-key used to encrypt this value. For
   *   example, `db_password`.</li>
   *   <li><b>Encryption Key Version:</b>The version of the private-key used to encrypt this value. For example, `1`
   *   .</li>
   *   <li><b>Ciphertext:</b> The encrypted ciphertext for the secret value, encoded as a Base64 string with padding.</li>
   * </ol>
   * </pre>
   *
   * An example of an encrypted secret encoded using this scheme is: `gcpkms:kr1:GSK:db_password:2:VGhpcyBpcyBhIHRoZSBzZWNyZXQ=`
   *
   * @return A {@link String} containing an encrypted secret encoded using this scheme.
   */
  String encodedValue();

  @Value.Immutable
  abstract class AbstractGcpEncodedSecret implements GcpEncodedSecret {

    @Override
    @Value.Derived
    public String keyringIdentifier() {
      try {
        return this.encodedValue().split(":")[1];
      } catch (ArrayIndexOutOfBoundsException e) {
        throw new IllegalStateException("Invalid GcpEncodedSecret string value!", e);
      }
    }

    @Override
    @Value.Derived
    public GcpEncryptionAlgorithm encryptionAlgorithm() {
      try {
        final String algId = this.encodedValue().split(":")[2];
        return GcpEncryptionAlgorithm.fromEncodedValue(algId);
      } catch (ArrayIndexOutOfBoundsException e) {
        throw new IllegalStateException("Invalid GcpEncodedSecret string value!", e);
      }
    }

    @Override
    @Value.Derived
    public String keyIdentifier() {
      try {
        return this.encodedValue().split(":")[3];
      } catch (ArrayIndexOutOfBoundsException e) {
        throw new IllegalStateException("Invalid GcpEncodedSecret string value!", e);
      }
    }

    @Override
    @Value.Derived
    public String keyVersion() {
      try {
        return this.encodedValue().split(":")[4];
      } catch (ArrayIndexOutOfBoundsException e) {
        throw new IllegalStateException("Invalid GcpEncodedSecret string value!", e);
      }
    }

    @Override
    @Value.Derived
    public String cipherText() {
      try {
        return this.encodedValue().split(":")[5];
      } catch (ArrayIndexOutOfBoundsException e) {
        throw new IllegalStateException("Invalid GcpEncodedSecret string value!", e);
      }
    }

    @Override
    public String toString() {
      return this.encodedValue();
    }
  }

}
