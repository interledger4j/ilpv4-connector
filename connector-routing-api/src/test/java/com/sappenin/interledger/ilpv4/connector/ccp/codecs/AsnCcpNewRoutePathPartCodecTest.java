package com.sappenin.interledger.ilpv4.connector.ccp.codecs;

import com.google.common.io.BaseEncoding;
import com.sappenin.interledger.ilpv4.connector.ccp.CcpRoutePathPart;
import com.sappenin.interledger.ilpv4.connector.ccp.ImmutableCcpRoutePathPart;
import org.interledger.core.InterledgerAddress;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

/**
 * Unit tests for {@link AsnCcpRoutePathPartCodec} that tests the encoding/decoding.
 */
@RunWith(Parameterized.class)
public class AsnCcpNewRoutePathPartCodecTest extends AbstractAsnCodecTest<CcpRoutePathPart> {

  /**
   * Construct an instance of this parameterized test with the supplied inputs.
   *
   * @param expectedCcpRoutePathPart
   * @param asn1OerBytes             The expected value, in binary, of the supplied {@code expectedPayloadLength}.
   */
  public AsnCcpNewRoutePathPartCodecTest(
    final CcpRoutePathPart expectedCcpRoutePathPart, final byte[] asn1OerBytes
  ) {
    super(expectedCcpRoutePathPart, asn1OerBytes, CcpRoutePathPart.class);
  }

  /**
   * The data for this test...
   */
  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        {
          ImmutableCcpRoutePathPart.builder().routePathPart(InterledgerAddress.of("example.prefix1")).build(),
          BaseEncoding.base16().decode("0F6578616D706C652E70726566697831")
        },
        {
          ImmutableCcpRoutePathPart.builder().routePathPart(InterledgerAddress.of("example.prefix3")).build(),
          BaseEncoding.base16().decode("0F6578616D706C652E70726566697833")
        }
      }
    );
  }
}