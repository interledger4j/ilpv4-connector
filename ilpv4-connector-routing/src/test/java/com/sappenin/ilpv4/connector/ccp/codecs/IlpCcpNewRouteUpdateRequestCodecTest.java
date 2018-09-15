package com.sappenin.ilpv4.connector.ccp.codecs;

import com.google.common.collect.Lists;
import com.google.common.io.BaseEncoding;
import com.sappenin.ilpv4.connector.ccp.*;
import com.sappenin.ilpv4.model.RoutingTableId;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerPreparePacket;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Unit tests for {@link AsnCcpRouteUpdateRequestCodec} that tests encoding/decoding of a Route control payload inside
 * of an ILP packet. These tests are patterned off of the ILP JS Tests for compatibility purposes.
 *
 * @see "https://github.com/interledgerjs/ilp-protocol-ccp/blob/master/test/index.test.ts"
 */
@RunWith(Parameterized.class)
public class IlpCcpNewRouteUpdateRequestCodecTest extends AbstractAsnCodecTest<InterledgerPreparePacket> {

  private static final String EXECUTION_CONDITION_HEX =
    "66687AADF862BD776C8FC18B8E9F8E20089714856EE233B3902A591D0D5F2925";

  private static final InterledgerAddress PEER_ROUTE_UPDATE_DESTINATION = InterledgerAddress.of("peer.getRoute.update");

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
  public IlpCcpNewRouteUpdateRequestCodecTest(
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

      // 0 - JS Compatibility (simple request)
      {
        InterledgerPreparePacket.builder()
          .destination(PEER_ROUTE_UPDATE_DESTINATION)
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
                  ImmutableCcpRouteUpdateRequest.builder()
                    .routingTableId(RoutingTableId.of(UUID.fromString("21e55f8e-abcd-4e97-9ab9-bf0ff00a224c")))
                    .currentEpochIndex(52)
                    .fromEpochIndex(52)
                    .toEpochIndex(52)
                    .holdDownTime(30000)
                    .speaker(InterledgerAddress.of("example.alice"))
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
          "0C7E0000000000000000323031353036313630303031303030303066687AADF862BD776C8FC18B8E9F8E20089714856EE233B3" +
            "902A591D0D5F292511706565722E726F7574652E7570646174653221E55F8EABCD4E979AB9BF0FF00A224C000000340000003400000" +
            "034000075300D6578616D706C652E616C69636501000100")
      },

      // 0 - JS Compatibility (complex request)
      {
        InterledgerPreparePacket.builder()
          .destination(PEER_ROUTE_UPDATE_DESTINATION)
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
                  ImmutableCcpRouteUpdateRequest.builder()
                    .routingTableId(RoutingTableId.of(UUID.fromString("bffbf6ad-0ddc-4d3b-a1e5-b4f0537365bd")))
                    .currentEpochIndex(52)
                    .fromEpochIndex(46)
                    .toEpochIndex(50)
                    .holdDownTime(30000)
                    .speaker(InterledgerAddress.of("example.alice"))
                    .newRoutes(Lists.newArrayList(
                      ImmutableCcpNewRoute.builder()
                        .prefix(InterledgerAddressPrefix.of("example.prefix1"))
                        .path(
                          Lists.newArrayList(
                            ImmutableCcpRoutePathPart.builder()
                              .routePathPart(InterledgerAddress.of("example.prefix1"))
                              .build()
                          )
                        )
                        .auth(Base64.getDecoder().decode("emx9hYZ8RqL6v60a+npKXiKc5XT8zmP17e7fwD+EaOo="))
                        // empty properties.
                        .build(),
                      ImmutableCcpNewRoute.builder()
                        .prefix(InterledgerAddressPrefix.of("example.prefix2"))
                        .path(
                          Lists.newArrayList(
                            ImmutableCcpRoutePathPart.builder()
                              .routePathPart(InterledgerAddress.of("example.connector1"))
                              .build(),
                            ImmutableCcpRoutePathPart.builder()
                              .routePathPart(InterledgerAddress.of("example.prefix2"))
                              .build()
                          )
                        )
                        .auth(Base64.getDecoder().decode("KwjlP7zBfF8b1Urg2a17o5pfmnsSbKm1wJRWCaNTJMw="))
                        .properties(
                          Lists.newArrayList(
                            ImmutableCcpRouteProperty.builder()
                              .id(0)
                              .optional(false)
                              .transitive(true)
                              .partial(false)
                              .utf8(true)
                              .value("hello world".getBytes())
                              .build(),
                            ImmutableCcpRouteProperty.builder()
                              .id(1)
                              .optional(true)
                              .transitive(true)
                              .partial(true)
                              .utf8(false)
                              .value(BaseEncoding.base16().decode("A0A0A0A0"))
                              .build()
                          )
                        )
                        .build()
                    ))
                    .withdrawnRoutePrefixes(
                      Lists.newArrayList(
                        ImmutableCcpWithdrawnRoute.builder().prefix(InterledgerAddressPrefix.of("example.prefix3"))
                          .build(),
                        ImmutableCcpWithdrawnRoute.builder().prefix(InterledgerAddressPrefix.of("example.prefix4"))
                          .build()
                      )
                    )
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
          "0C8201520000000000000000323031353036313630303031303030303066687AADF862BD776C8FC18B8E9F8E20089714856EE233B3902A591D0D5F292511706565722E726F7574652E757064617465820104BFFBF6AD0DDC4D3BA1E5B4F0537365BD000000340000002E00000032000075300D6578616D706C652E616C69636501020F6578616D706C652E7072656669783101010F6578616D706C652E707265666978317A6C7D85867C46A2FABFAD1AFA7A4A5E229CE574FCCE63F5EDEEDFC03F8468EA01000F6578616D706C652E707265666978320102126578616D706C652E636F6E6E6563746F72310F6578616D706C652E707265666978322B08E53FBCC17C5F1BD54AE0D9AD7BA39A5F9A7B126CA9B5C0945609A35324CC01025000000B68656C6C6F20776F726C64E0000104A0A0A0A001020F6578616D706C652E707265666978330F6578616D706C652E70726566697834")
      },
    });
  }

}