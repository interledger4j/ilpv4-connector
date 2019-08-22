package com.sappenin.interledger.ilpv4.connector.ccp.codecs;

import com.google.common.io.BaseEncoding;
import com.sappenin.interledger.ilpv4.connector.ccp.CcpRouteControlResponse;
import com.sappenin.interledger.ilpv4.connector.ccp.ImmutableCcpRouteControlResponse;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

/**
 * Unit tests for {@link AsnCcpRouteControlResponseCodec} that tests the encoding/decoding in the absence of an ILP
 * packet.
 */
@RunWith(Parameterized.class)
public class AsnCcpNewRouteControlResponseCodecTest extends AbstractAsnCodecTest<CcpRouteControlResponse> {

  /**
   * Construct an instance of this parameterized test with the supplied inputs.
   *
   * @param expectedResponse
   * @param asn1OerBytes     The expected value, in binary, of the supplied {@code expectedPayloadLength}.
   */
  public AsnCcpNewRouteControlResponseCodecTest(
    final CcpRouteControlResponse expectedResponse, final byte[] asn1OerBytes
  ) {
    super(expectedResponse, asn1OerBytes, CcpRouteControlResponse.class);
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
        ImmutableCcpRouteControlResponse.builder().build(),
        BaseEncoding.base16().decode("")
      }
    });
  }

}