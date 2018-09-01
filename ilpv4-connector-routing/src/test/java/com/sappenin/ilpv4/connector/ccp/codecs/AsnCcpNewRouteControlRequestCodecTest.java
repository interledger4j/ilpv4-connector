package com.sappenin.ilpv4.connector.ccp.codecs;

import com.google.common.collect.Lists;
import com.google.common.io.BaseEncoding;
import com.sappenin.ilpv4.connector.ccp.CcpSyncMode;
import com.sappenin.ilpv4.connector.ccp.CcpRouteControlRequest;
import com.sappenin.ilpv4.connector.ccp.ImmutableCcpRouteControlRequest;
import com.sappenin.ilpv4.connector.ccp.ImmutableCcpFeature;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

/**
 * Unit tests for {@link AsnCcpRouteControlRequestCodec} that tests the encoding/decoding in the absence of an ILP
 * packet.
 */
@RunWith(Parameterized.class)
public class AsnCcpNewRouteControlRequestCodecTest extends AbstractAsnCodecTest<CcpRouteControlRequest> {

  /**
   * Construct an instance of this parameterized test with the supplied inputs.
   *
   * @param expectedCcpRouteControlRequest
   * @param asn1OerBytes                   The expected value, in binary, of the supplied {@code
   *                                       expectedPayloadLength}.
   */
  public AsnCcpNewRouteControlRequestCodecTest(
    final CcpRouteControlRequest expectedCcpRouteControlRequest, final byte[] asn1OerBytes
  ) {
    super(expectedCcpRouteControlRequest, asn1OerBytes, CcpRouteControlRequest.class);
  }

  /**
   * The data for this test...
   */
  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
      // [ccp_control_request][byte_values]

      // 0 - JS Compatibility (1)
      {ImmutableCcpRouteControlRequest.builder()
        .mode(CcpSyncMode.MODE_SYNC)
        .lastKnownRoutingTableId(UUID.fromString("70d1a134-a0df-4f47-964f-6e19e2ab3790"))
        .lastKnownEpoch(32)
        .features(Lists.newArrayList(
          ImmutableCcpFeature.builder().value("foo").build(),
          ImmutableCcpFeature.builder().value("bar").build()
        ))
        .build(),
        BaseEncoding.base16().decode("0170D1A134A0DF4F47964F6E19E2AB379000000020010203666F6F03626172")},

      // No Features...
      {ImmutableCcpRouteControlRequest.builder()
        .mode(CcpSyncMode.MODE_SYNC)
        .lastKnownRoutingTableId(UUID.fromString("a1e2f8ba-d5cf-479e-a975-6f2cd0caf4a2"))
        .lastKnownEpoch(32)
        .build(),
        BaseEncoding.base16().decode("01A1E2F8BAD5CF479EA9756F2CD0CAF4A2000000200100")},

      // Other Mode + 0 Epoch
      {ImmutableCcpRouteControlRequest.builder()
        .mode(CcpSyncMode.MODE_IDLE)
        .lastKnownRoutingTableId(UUID.fromString("70d1a134-a0df-4f47-964f-6e19e2ab3790"))
        .lastKnownEpoch(0)
        .build(),
        BaseEncoding.base16().decode("0070D1A134A0DF4F47964F6E19E2AB3790000000000100")},
    });
  }

}