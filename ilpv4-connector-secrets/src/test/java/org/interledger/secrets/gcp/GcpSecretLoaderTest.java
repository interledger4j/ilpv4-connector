package org.interledger.secrets.gcp;

import com.google.protobuf.ByteString;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Unit test for {@link GcpSecretLoader}.
 */
public class GcpSecretLoaderTest {

  private GcpSecretLoader gcpSecretLoader;

  @Before
  public void setUp() throws Exception {
    this.gcpSecretLoader = new GcpSecretLoader("ilpv4-dev", "global");
  }

  @Test
  public void loadSecretFromGoogleKms() {
    final GcpEncodedSecret gcpEncodedSecret = GcpEncodedSecret.builder()
      .encodedValue(
        "enc:marty:GS:test_key:1:CiQA+jwkPUqrXgfiJ0Tj5YFNvq/Dw9n1drcRBXMrTpAyTMyBdTYSQgA+wudV0aDCY8qz8Cb+RgBsXu5+vCykG83CljAM104ftSGeEfcQ4bcC02VnlbQSJTsxw4ktvwYKK1AKAi5BBZ+N4w=="
      )
      .build();

    final ByteString byteString = gcpSecretLoader.loadSecretFromGoogleKms(gcpEncodedSecret);
    assertThat(byteString.toStringUtf8(), is("Some text to be encrypted"));
  }
}