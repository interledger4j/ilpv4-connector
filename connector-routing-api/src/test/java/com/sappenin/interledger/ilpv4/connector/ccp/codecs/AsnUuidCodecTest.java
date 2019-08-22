package com.sappenin.interledger.ilpv4.connector.ccp.codecs;

import com.google.common.io.BaseEncoding;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

/**
 * Unit tests that test the encoding/decoding of a {@link UUID} to/from ASN.1 OER.
 */
@RunWith(Parameterized.class)
public class AsnUuidCodecTest extends AbstractAsnCodecTest<UUID> {

  /**
   * Construct an instance of this parameterized test with the supplied inputs.
   *
   * @param expectedUuid
   * @param asn1OerBytes The expected value, in binary, of the supplied {@code expectedPayloadLength}.
   */
  public AsnUuidCodecTest(final UUID expectedUuid, final byte[] asn1OerBytes) {
    super(expectedUuid, asn1OerBytes, UUID.class);
  }

  /**
   * The data for this test...
   */
  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
      // [ccp_control_request][byte_values]

      // 0 - JS Compatibility (1)
      {
        UUID.fromString("70d1a134-a0df-4f47-964f-6e19e2ab3790"),
        BaseEncoding.base16().decode("70D1A134A0DF4F47964F6E19E2AB3790")
      },
      // 1 - JS Compatibility (2)
      {
        UUID.fromString("21e55f8e-abcd-4e97-9ab9-bf0ff00a224c"),
        BaseEncoding.base16().decode("21E55F8EABCD4E979AB9BF0FF00A224C")
      }
    });
  }

  @Test
  public void testUUIDHas16Bytes() {
    final UUID uuid = UUID.randomUUID();
    final byte[] result = AsnUuidCodec.getBytesFromUUID(uuid);

    assertThat("Resulting byte array should have had 16 elements.", result.length, is(16));
  }

  @Test
  public void testSameUUIDFromByteArray() {
    final UUID uuid = UUID.randomUUID();
    byte[] bytes = AsnUuidCodec.getBytesFromUUID(uuid);
    final UUID reconstructedUuid = AsnUuidCodec.getUUIDFromBytes(bytes);

    assertThat(uuid, is(reconstructedUuid));
  }

  @Test
  public void testNotSameUUIDFromByteArray() {
    final UUID uuid = UUID.fromString("80de5eaf-9379-4cb5-aaa4-1250649326cc");
    final byte[] result = AsnUuidCodec.getBytesFromUUID(uuid);
    final UUID newUuid = UUID.nameUUIDFromBytes(result);

    assertThat(uuid, is(not(newUuid)));
  }
}