package org.interledger.crypto.gcp;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.crypto.EncryptedSecret;
import org.interledger.crypto.EncryptionAlgorithm;
import org.interledger.crypto.KeyMetadata;
import org.interledger.crypto.impl.GcpEncryptionService;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Unit test for {@link GcpEncryptionService}.
 */
public class GcpEncryptionServiceTest {

  private static final String TEST_SECRET = "shh";
  private GcpEncryptionService gcpEncryptionService;

  /**
   * Ignored because GCP credentials don't exist in CI. In order to run this test, see GCP instructions.
   *
   * @see "https://cloud.google.com/docs/authentication/production#obtaining_and_providing_service_account_credentials_manually"
   */
  @Test
  @Ignore
  public void encryptDecryptViaGoogleKms() {
    this.gcpEncryptionService = new GcpEncryptionService("ilpv4-dev", "global");

    final KeyMetadata metaData = KeyMetadata.builder()
      .platformIdentifier("gcp")
      .keyringIdentifier("marty")
      .keyVersion("1")
      .keyIdentifier("test_key")
      .build();

    final EncryptedSecret encryptedSecret = gcpEncryptionService.encrypt(
      metaData, EncryptionAlgorithm.GOOGLE_SYMMETRIC, TEST_SECRET.getBytes()
    );
    final EncryptedSecret derivedEncryptedSecret = EncryptedSecret.fromEncodedValue(encryptedSecret.encodedValue());
    assertThat(derivedEncryptedSecret).isEqualTo(encryptedSecret);

    final byte[] byteString = gcpEncryptionService.decrypt(encryptedSecret);
    assertThat(new String(byteString)).isEqualTo(TEST_SECRET);
  }
}