package org.interledger.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Base64;
import java.util.Collection;

@RunWith(Parameterized.class)
public class EncryptedSecretTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private final String encodedValueToTest;

  private final String expectedPlatformIdentifier;
  private final String expectedKeyringIdentifier;
  private final String expectedKeyIdentifier;
  private final String expectedKeyVersion;
  private final String expectedCipherText;
  private final EncryptionAlgorithm expectedGcpEncryptionAlgorithm;

  public EncryptedSecretTest(
    final String encodedValueToTest,
    final String expectedPlatformIdentifier,
    final String expectedKeyringIdentifier, final EncryptionAlgorithm expectedGcpEncryptionAlgorithm,
    final String expectedKeyIdentifier, final String expectedKeyVersion, final String expectedCipherText
  ) {
    this.encodedValueToTest = encodedValueToTest;
    this.expectedPlatformIdentifier = expectedPlatformIdentifier;
    this.expectedKeyringIdentifier = expectedKeyringIdentifier;
    this.expectedGcpEncryptionAlgorithm = expectedGcpEncryptionAlgorithm;
    this.expectedKeyIdentifier = expectedKeyIdentifier;
    this.expectedKeyVersion = expectedKeyVersion;
    this.expectedCipherText = expectedCipherText;
  }

  @Parameterized.Parameters
  public static Collection<Object[]> secretValues() {
    return ImmutableList.of(
      new Object[]{
        // Try upper-case to ensure normalization...
        EncryptedSecret.ENCODING_PREFIX + ":GCPKMS:KR1:Foo_password:1:GS:VGhpcyBpcyBhIHRoZSBzZWNyZXQ=",
        "gcpkms",
        "kr1",
        EncryptionAlgorithm.GOOGLE_SYMMETRIC,
        "Foo_password",
        "1",
        "VGhpcyBpcyBhIHRoZSBzZWNyZXQ="
      },
      new Object[]{
        EncryptedSecret.ENCODING_PREFIX +
          ":gcpkms:kr2:foo_pw:1:gs:CiQAUCbpRzRVsJV5zlnAGelriEXxwW7KBF1_WjukiQeTNx-vcIISQgDt51s4dnxxSB8oxHTDehlfAEDp2WbqOoui2yv92wIdnFT2SSbLQgpDWWOxeuTPNTOmUR30G6BdJpBoB2hQXrFa7A==",
        "gcpkms",
        "kr2",
        EncryptionAlgorithm.GOOGLE_SYMMETRIC,
        "foo_pw",
        "1",
        "CiQAUCbpRzRVsJV5zlnAGelriEXxwW7KBF1_WjukiQeTNx-vcIISQgDt51s4dnxxSB8oxHTDehlfAEDp2WbqOoui2yv92wIdnFT2SSbLQgpDWWOxeuTPNTOmUR30G6BdJpBoB2hQXrFa7A=="
      }
    );
  }

  @Test
  public void testUnspecifiedString() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Cannot build EncryptedSecret, some of required attributes are not set [keyMetadata, encryptionAlgorithm, cipherMessage]");
    EncryptedSecret.builder().build();
  }

  @Test
  public void testEmptyString() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("encodedValue must start with `enc` prefix");
    EncryptedSecret.fromEncodedValue("");
  }

  @Test
  public void testEncodedValueWithNoCipherText() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Invalid ciphertext!");
    EncryptedSecret.fromEncodedValue("enc:gcpkms:kr1:foo_password:1:gs:");
  }

  @Test
  public void testEncodedValueWithNoPrefix() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("encodedValue must start with `enc` prefix. Value was instead: gcpkms:kr1:foo_password:1:gs:VGhpcyBpcyBhIHRoZSBzZWNyZXQ=");
    EncryptedSecret.fromEncodedValue("gcpkms:kr1:foo_password:1:gs:VGhpcyBpcyBhIHRoZSBzZWNyZXQ=");
  }

  @Test
  public void testEncodedValueWithEmptyPrefix() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("encodedValue must start with `enc` prefix. Value was instead: :gcpkms:kr1:gs:foo_password:1:VGhpcyBpcyBhIHRoZSBzZWNyZXQ=");
    EncryptedSecret.fromEncodedValue(":gcpkms:kr1:gs:foo_password:1:VGhpcyBpcyBhIHRoZSBzZWNyZXQ=");
  }

  @Test
  public void testEncodedValueWithInvalidPrefix() {
      expectedException.expect(IllegalArgumentException.class);
      expectedException.expectMessage("encodedValue must start with `enc` prefix. Value was instead: enc2:gcpkms:kr1:gs:foo_password:1:VGhpcyBpcyBhIHRoZSBzZWNyZXQ=");
      EncryptedSecret.fromEncodedValue("enc2:gcpkms:kr1:gs:foo_password:1:VGhpcyBpcyBhIHRoZSBzZWNyZXQ=");
  }

  @Test
  public void testEncodedValueWithNoKeyIdentifier() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("keyIdentifier must not be empty");
    EncryptedSecret.fromEncodedValue(EncryptedSecret.ENCODING_PREFIX + ":gcpkms:kr1::1:gs:ct");
  }


  @Test
  public void testEncodedValueWithNoKeyringId() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("keyringIdentifier must not be empty");
    EncryptedSecret.fromEncodedValue(
      EncryptedSecret.ENCODING_PREFIX + ":gcpkms::foo_password:1:gs:VGhpcyBpcyBhIHRoZSBzZWNyZXQ=)"
    );
  }

  @Test
  public void testEncodedValueWithNoEncryptionAlg() {
    expectedException.expect(RuntimeException.class);
    expectedException.expectMessage("Invalid Encryption Algorithm: ");
    EncryptedSecret.fromEncodedValue(
      EncryptedSecret.ENCODING_PREFIX + ":gcpkms:kr1:foo_password:1::VGhpcyBpcyBhIHRoZSBzZWNyZXQ=)"
    );
  }

  @Test
  public void testEncodedValueWithNoKeyId() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("keyIdentifier must not be empty");
    EncryptedSecret.fromEncodedValue(EncryptedSecret.ENCODING_PREFIX + ":gcpkms:kr1::1:gs:ciphertext");
  }

  @Test
  public void testEncryptedSecretFrom() {
    final EncryptedSecret metadata = EncryptedSecret.fromEncodedValue(encodedValueToTest);

    assertThat(metadata.keyMetadata().platformIdentifier()).isEqualTo(this.expectedPlatformIdentifier);
    assertThat(metadata.keyMetadata().keyringIdentifier()).isEqualTo(this.expectedKeyringIdentifier);
    assertThat(metadata.keyMetadata().keyIdentifier()).isEqualTo(this.expectedKeyIdentifier);
    assertThat(metadata.keyMetadata().keyVersion()).isEqualTo(this.expectedKeyVersion);
    assertThat(metadata.encryptionAlgorithm()).isEqualTo(this.expectedGcpEncryptionAlgorithm);
    assertThat(Base64.getUrlEncoder().encodeToString(metadata.cipherMessage())).isEqualTo(this.expectedCipherText);
  }


  @Test
  public void testEncodedString() {
    final String actualEncodedValue = EncryptedSecret.ENCODING_PREFIX
      + ":gcpkms:kr1:foo_password:1:gs:VGhpcyBpcyBhIHRoZSBzZWNyZXQ=";
    final EncryptedSecret gcpEncodedSecret = EncryptedSecret.fromEncodedValue(actualEncodedValue);

    assertThat(gcpEncodedSecret.keyMetadata().platformIdentifier()).isEqualTo("gcpkms");
    assertThat(gcpEncodedSecret.keyMetadata().keyringIdentifier()).isEqualTo("kr1");
    assertThat(gcpEncodedSecret.keyMetadata().keyIdentifier()).isEqualTo("foo_password");
    assertThat(gcpEncodedSecret.keyMetadata().keyVersion()).isEqualTo("1");
    assertThat(gcpEncodedSecret.encryptionAlgorithm()).isEqualTo(EncryptionAlgorithm.GOOGLE_SYMMETRIC);
    assertThat(Base64.getUrlEncoder().encodeToString(gcpEncodedSecret.cipherMessage())).isEqualTo("VGhpcyBpcyBhIHRoZSBzZWNyZXQ=");
    assertThat(gcpEncodedSecret.encodedValue()).isEqualTo(actualEncodedValue);
    assertThat(gcpEncodedSecret.toString()).isEqualTo(actualEncodedValue);
  }

}