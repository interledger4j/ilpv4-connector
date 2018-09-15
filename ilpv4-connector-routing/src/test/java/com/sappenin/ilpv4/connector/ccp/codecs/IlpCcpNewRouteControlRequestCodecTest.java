package com.sappenin.ilpv4.connector.ccp.codecs;

import com.google.common.collect.Lists;
import com.google.common.io.BaseEncoding;
import com.sappenin.ilpv4.connector.ccp.CcpRouteControlRequest;
import com.sappenin.ilpv4.connector.ccp.CcpSyncMode;
import com.sappenin.ilpv4.connector.ccp.ImmutableCcpFeature;
import com.sappenin.ilpv4.connector.ccp.ImmutableCcpRouteControlRequest;
import com.sappenin.ilpv4.model.RoutingTableId;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerPreparePacket;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Unit tests for {@link AsnCcpRouteControlRequestCodec} that tests encoding/decoding of a control payload inside of an
 * ILP packet. These tests are patterned off of the ILP JS Tests for compatibility purposes.
 *
 * @see "https://github.com/interledgerjs/ilp-protocol-ccp/blob/master/test/index.test.ts"
 */
@RunWith(Parameterized.class)
public class IlpCcpNewRouteControlRequestCodecTest extends AbstractAsnCodecTest<InterledgerPreparePacket> {

  private static final String EXECUTION_CONDITION_HEX =
    "66687AADF862BD776C8FC18B8E9F8E20089714856EE233B3902A591D0D5F2925";

  private static final InterledgerAddress PEER_ROUTE_CONTROL_DESTINATION =
    InterledgerAddress.of("peer.getRoute.control");

  // Matches the JS tests in ILP JS which says, June 16, 2015 00:00:00 GMT
  // However, the value used in that test is off by 1 minute.
  private static final Instant INSTANT = Instant.ofEpochMilli(1434412800000L).plusSeconds(60);

  /**
   * Construct an instance of this parameterized test with the supplied inputs.
   *
   * @param expectedPreparePacket An {@link InterledgerPreparePacket} with a data-payload of a {@link
   *                              CcpRouteControlRequest}.
   * @param asn1OerBytes          The expected value, in binary, of {@code expectedPreparePacket}.
   */
  public IlpCcpNewRouteControlRequestCodecTest(
    final InterledgerPreparePacket expectedPreparePacket, final byte[] asn1OerBytes
  ) {
    super(expectedPreparePacket, asn1OerBytes, InterledgerPreparePacket.class);
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
        InterledgerPreparePacket.builder()
          .destination(PEER_ROUTE_CONTROL_DESTINATION)
          .amount(BigInteger.ZERO)
          .executionCondition(
            InterledgerCondition.of(BaseEncoding.base16().decode(EXECUTION_CONDITION_HEX))
          )
          .expiresAt(INSTANT)
          .data(
            ((Supplier<byte[]>) () -> {
              final ByteArrayOutputStream baos = new ByteArrayOutputStream();
              try {
                codecContext.write(
                  ImmutableCcpRouteControlRequest.builder()
                    .mode(CcpSyncMode.MODE_SYNC)
                    .lastKnownRoutingTableId(RoutingTableId.of(UUID.fromString("70d1a134-a0df-4f47-964f-6e19e2ab3790")))
                    .lastKnownEpoch(32)
                    .features(Lists.newArrayList(
                      ImmutableCcpFeature.builder().value("foo").build(),
                      ImmutableCcpFeature.builder().value("bar").build()
                    ))
                    .build()
                  , baos);
                return baos.toByteArray();
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            }).get()
          )
          .build(),
        BaseEncoding.base16().decode(
          "0C6C0000000000000000323031353036313630303031303030303066687AADF862BD776C8FC18B8E9F8E20089714856EE233B3" +
            "902A591D0D5F292512706565722E726F7574652E636F6E74726F6C1F0170D1A134A0DF4F47964F6E19E2AB379000000020010203666F6F03626172")},
    });
  }

}