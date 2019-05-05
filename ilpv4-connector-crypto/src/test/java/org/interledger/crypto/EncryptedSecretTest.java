package org.interledger.crypto;

import com.google.common.collect.ImmutableList;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Base64;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class EncryptedSecretTest {

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

  @Test(expected = IllegalStateException.class)
  public void testUnspecifiedString() {
    try {
      EncryptedSecret.builder().build();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage(),
        is(
          "Cannot build EncryptedSecret, some of required attributes are not set [keyMetadata, encryptionAlgorithm, cipherMessage]"));
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyString() {
    try {
      EncryptedSecret.fromEncodedValue("");
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage(), is("encodedValue must start with `enc` prefix"));
      throw e;
    }
  }

  @Test(expected = IllegalStateException.class)
  public void testEncodedValueWithNoCipherText() {
    try {
      EncryptedSecret.fromEncodedValue("enc:gcpkms:kr1:foo_password:1:gs:");
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage(), is("Invalid ciphertext!"));
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEncodedValueWithNoPrefix() {
    try {
      EncryptedSecret.fromEncodedValue("gcpkms:kr1:foo_password:1:gs:VGhpcyBpcyBhIHRoZSBzZWNyZXQ=");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is("encodedValue must start with `enc` prefix"));
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEncodedValueWithEmptyPrefix() {
    try {
      EncryptedSecret.fromEncodedValue(":gcpkms:kr1:gs:foo_password:1:VGhpcyBpcyBhIHRoZSBzZWNyZXQ=");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is("encodedValue must start with `enc` prefix"));
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEncodedValueWithInvalidPrefix() {
    try {
      EncryptedSecret.fromEncodedValue("enc2:gcpkms:kr1:gs:foo_password:1:VGhpcyBpcyBhIHRoZSBzZWNyZXQ=");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is("encodedValue must start with `enc` prefix"));
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEncodedValueWithNoKeyIdentifier() {
    try {
      EncryptedSecret.fromEncodedValue(EncryptedSecret.ENCODING_PREFIX + ":gcpkms:kr1::1:gs:ct");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is("keyIdentifier must not be empty"));
      throw e;
    }
  }


  @Test(expected = IllegalArgumentException.class)
  public void testEncodedValueWithNoKeyringId() {
    try {
      EncryptedSecret.fromEncodedValue(
        EncryptedSecret.ENCODING_PREFIX + ":gcpkms::foo_password:1:gs:VGhpcyBpcyBhIHRoZSBzZWNyZXQ=)"
      );
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage(), is("keyringIdentifier must not be empty"));
      throw e;
    }
  }

  @Test(expected = RuntimeException.class)
  public void testEncodedValueWithNoEncryptionAlg() {
    try {
      EncryptedSecret.fromEncodedValue(
        EncryptedSecret.ENCODING_PREFIX + ":gcpkms:kr1:foo_password:1::VGhpcyBpcyBhIHRoZSBzZWNyZXQ=)"
      );
      fail();
    } catch (RuntimeException e) {
      assertThat(e.getMessage(), is("Invalid Encryption Algorithm: "));
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEncodedValueWithNoKeyId() {
    try {
      EncryptedSecret.fromEncodedValue(EncryptedSecret.ENCODING_PREFIX + ":gcpkms:kr1::1:gs:ciphertext");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is("keyIdentifier must not be empty"));
      throw e;
    }
  }

  @Test
  public void testEncryptedSecretFrom() {
    final EncryptedSecret metadata = EncryptedSecret.fromEncodedValue(encodedValueToTest);

    assertThat(metadata.keyMetadata().platformIdentifier(), is(this.expectedPlatformIdentifier));
    assertThat(metadata.keyMetadata().keyringIdentifier(), is(this.expectedKeyringIdentifier));
    assertThat(metadata.keyMetadata().keyIdentifier(), is(this.expectedKeyIdentifier));
    assertThat(metadata.keyMetadata().keyVersion(), is(this.expectedKeyVersion));
    assertThat(metadata.encryptionAlgorithm(), is(this.expectedGcpEncryptionAlgorithm));
    assertThat(Base64.getUrlEncoder().encodeToString(metadata.cipherMessage()), is(this.expectedCipherText));
  }


  @Test
  public void testEncodedString() {
    final String actualEncodedValue = EncryptedSecret.ENCODING_PREFIX
      + ":gcpkms:kr1:foo_password:1:gs:VGhpcyBpcyBhIHRoZSBzZWNyZXQ=";
    final EncryptedSecret gcpEncodedSecret = EncryptedSecret.fromEncodedValue(actualEncodedValue);

    assertThat(gcpEncodedSecret.keyMetadata().platformIdentifier(), is("gcpkms"));
    assertThat(gcpEncodedSecret.keyMetadata().keyringIdentifier(), is("kr1"));
    assertThat(gcpEncodedSecret.keyMetadata().keyIdentifier(), is("foo_password"));
    assertThat(gcpEncodedSecret.keyMetadata().keyVersion(), is("1"));
    assertThat(gcpEncodedSecret.encryptionAlgorithm(), Is.is(EncryptionAlgorithm.GOOGLE_SYMMETRIC));
    assertThat(Base64.getUrlEncoder().encodeToString(gcpEncodedSecret.cipherMessage()),
      is("VGhpcyBpcyBhIHRoZSBzZWNyZXQ="));
    assertThat(gcpEncodedSecret.encodedValue(), is(actualEncodedValue));
    assertThat(gcpEncodedSecret.toString(), is(actualEncodedValue));
  }

}