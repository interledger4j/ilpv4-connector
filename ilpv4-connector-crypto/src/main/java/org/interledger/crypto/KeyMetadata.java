package org.interledger.crypto;

import com.google.common.base.Preconditions;
import org.immutables.value.Value;

/**
 * Metadata about a particular encrypted secret.
 */
public interface KeyMetadata {

  static ImmutableKeyMetadata.Builder builder() {
    return ImmutableKeyMetadata.builder();
  }

  /**
   * The unique identifier of the platform that can decode this secret.
   *
   * @return
   */
  String platformIdentifier();

  /**
   * The unique identifier of the keyring that holds the the private-key used to encrypt this encoded secret.
   *
   * @return
   */
  String keyringIdentifier();

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

  @Value.Immutable
  abstract class AbstractKeyMetadata implements KeyMetadata {

    @Value.Check
    public void doChecks() {
      Preconditions.checkArgument(platformIdentifier().length() > 0, "platformIdentifier must not be empty");
      Preconditions.checkArgument(keyringIdentifier().length() > 0, "keyringIdentifier must not be empty");
      Preconditions.checkArgument(keyIdentifier().length() > 0, "keyIdentifier must not be empty");
      Preconditions.checkArgument(keyVersion().length() > 0, "keyVersion must not be empty");
    }
  }

}
