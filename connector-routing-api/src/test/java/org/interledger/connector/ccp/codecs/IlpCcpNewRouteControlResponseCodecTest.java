package org.interledger.connector.ccp.codecs;

import com.google.common.io.BaseEncoding;
import org.interledger.connector.ccp.CcpRouteControlResponse;
import org.interledger.connector.ccp.ImmutableCcpRouteControlResponse;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerFulfillment;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Supplier;

/**
 * Unit tests for {@link AsnCcpRouteControlRequestCodec} that tests encoding/decoding of a control payload inside of an
 * ILP packet. These tests are patterned off of the ILP JS Tests for compatibility purposes.
 *
 * @see "https://github.com/interledgerjs/ilp-protocol-ccp/blob/master/test/index.test.ts"
 */
@RunWith(Parameterized.class)
public class IlpCcpNewRouteControlResponseCodecTest extends AbstractAsnCodecTest<InterledgerFulfillPacket> {

  private static final String FULFILLMENT_HEX =
    "0000000000000000000000000000000000000000000000000000000000000000";

  /**
   * Construct an instance of this parameterized test with the supplied inputs.
   *
   * @param expectedFulfillPacket An {@link InterledgerFulfillPacket} with a data-payload of a {@link
   *                              CcpRouteControlResponse}.
   * @param asn1OerBytes          The expected value, in binary, of {@code expectedFulfillPacket}.
   */
  public IlpCcpNewRouteControlResponseCodecTest(
    final InterledgerFulfillPacket expectedFulfillPacket, final byte[] asn1OerBytes
  ) {
    super(expectedFulfillPacket, asn1OerBytes, InterledgerFulfillPacket.class);
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
        InterledgerFulfillPacket.builder()
          .fulfillment(
            InterledgerFulfillment.of(BaseEncoding.base16().decode(FULFILLMENT_HEX))
          )
          .data(BaseEncoding.base16().decode("0E6578616D706C652E636C69656E740D0358414D"))
          .build(),
        BaseEncoding.base16().decode(
          "0D350000000000000000000000000000000000000000000000000000000000000000140E6578616D706C652E636C69656E740D0358414D")
      },
      // 1 - JS Compatibility (1)
      {
        InterledgerFulfillPacket.builder()
          .fulfillment(
            InterledgerFulfillment.of(BaseEncoding.base16().decode(FULFILLMENT_HEX))
          )
          .data(
            ((Supplier<byte[]>) () -> {
              final ByteArrayOutputStream baos = new ByteArrayOutputStream();
              try {
                codecContext.write(ImmutableCcpRouteControlResponse.builder().build(), baos);
                return baos.toByteArray();
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            }).get()
          )
          .build(),
        BaseEncoding.base16().decode("0D21000000000000000000000000000000000000000000000000000000000000000000")
      }
    });
  }
}

