package org.interledger.secrets;

import com.google.common.collect.ImmutableList;
import org.interledger.secrets.gcp.GcpEncodedSecret;
import org.interledger.secrets.gcp.GcpEncryptionAlgorithm;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.interledger.secrets.gcp.GcpEncryptionAlgorithm.GOOGLE_SYMMETRIC;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class GcpEncodedSecretTest {

  private final String actualSecretValue;
  private final String expectedKeyringIdentifier;
  private final GcpEncryptionAlgorithm expectedGcpEncryptionAlgorithm;
  private final String expectedKeyIdentifier;
  private final String expectedKeyVersion;
  private final String expectedCipherText;

  public GcpEncodedSecretTest(
    final String actualSecretValue,
    final String expectedKeyringIdentifier, final GcpEncryptionAlgorithm expectedGcpEncryptionAlgorithm,
    final String expectedKeyIdentifier, final String expectedKeyVersion, final String expectedCipherText
  ) {
    this.actualSecretValue = actualSecretValue;
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
        "enc:kr1:GS:foo_password:1:VGhpcyBpcyBhIHRoZSBzZWNyZXQ=", "kr1", GOOGLE_SYMMETRIC, "foo_password", "1",
        "VGhpcyBpcyBhIHRoZSBzZWNyZXQ="
      },
      new Object[]{
        "gcpkms:kr2:GS:bar_password:2:VGhpcyBpcyBhIHRoZSBvdGhlciBzZWNyZXQ=", "kr2", GOOGLE_SYMMETRIC, "bar_password",
        "2", "VGhpcyBpcyBhIHRoZSBvdGhlciBzZWNyZXQ="
      },
      // Empty values
      new Object[]{
        "gcpkms::GS:::VGhpcyBpcyBhIHRoZSBvdGhlciBzZWNyZXQ=", "", GOOGLE_SYMMETRIC, "", "",
        "VGhpcyBpcyBhIHRoZSBvdGhlciBzZWNyZXQ="
      },
      new Object[]{
        "gcpkms:kr2:GS:foo_pw:1:CiQAUCbpRzRVsJV5zlnAGelriEXxwW7KBF1/WjukiQeTNx" +
          "+vcIISQgDt51s4dnxxSB8oxHTDehlfAEDp2WbqOoui2yv92wIdnFT2SSbLQgpDWWOxeuTPNTOmUR30G6BdJpBoB2hQXrFa7A==", "kr2",
        GOOGLE_SYMMETRIC, "foo_pw", "1",
        "CiQAUCbpRzRVsJV5zlnAGelriEXxwW7KBF1/WjukiQeTNx+vcIISQgDt51s4dnxxSB8oxHTDehlfAEDp2WbqOoui2yv92wIdnFT2SSbLQgpDWWOxeuTPNTOmUR30G6BdJpBoB2hQXrFa7A=="
      }
    );
  }

  @Test(expected = IllegalStateException.class)
  public void testUnspecifiedString() {
    try {
      GcpEncodedSecret.builder().build();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage(),
        is("Cannot build GcpEncodedSecret, some of required attributes are not set [encodedValue]"));
      throw e;
    }
  }

  @Test(expected = IllegalStateException.class)
  public void testEmptyString() {
    try {
      GcpEncodedSecret.builder()
        .encodedValue("")
        .build();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage(), is("Invalid GcpEncodedSecret string value!"));
      throw e;
    }
  }

  @Test(expected = IllegalStateException.class)
  public void testEncodedValueWithNoCipherText() {
    try {
      GcpEncodedSecret.builder()
        .encodedValue("gcpkms::GS:::")
        .build();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage(), is("Invalid GcpEncodedSecret string value!"));
      throw e;
    }
  }

  @Test
  public void testEncodedString() {
    final GcpEncodedSecret gcpEncodedSecret = GcpEncodedSecret.builder()
      .encodedValue(actualSecretValue)
      .build();

    assertThat(gcpEncodedSecret.keyringIdentifier(), is(expectedKeyringIdentifier));
    assertThat(gcpEncodedSecret.encryptionAlgorithm(), is(expectedGcpEncryptionAlgorithm));
    assertThat(gcpEncodedSecret.keyIdentifier(), is(expectedKeyIdentifier));
    assertThat(gcpEncodedSecret.keyVersion(), is(expectedKeyVersion));
    assertThat(gcpEncodedSecret.cipherText(), is(expectedCipherText));
    assertThat(gcpEncodedSecret.encodedValue(), is(actualSecretValue));
    assertThat(gcpEncodedSecret.toString(), is(actualSecretValue));
  }

}