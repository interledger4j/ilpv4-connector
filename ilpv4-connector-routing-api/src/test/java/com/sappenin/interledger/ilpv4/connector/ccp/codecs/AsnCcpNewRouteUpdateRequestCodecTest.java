package com.sappenin.interledger.ilpv4.connector.ccp.codecs;

import com.google.common.collect.Lists;
import com.google.common.io.BaseEncoding;
import com.sappenin.interledger.ilpv4.connector.ccp.*;
import com.sappenin.interledger.ilpv4.connector.routing.RoutingTableId;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.UUID;

/**
 * Unit tests for {@link AsnCcpRouteUpdateRequestCodec} that tests the encoding/decoding in the absence of an ILP
 * packet.
 */
@RunWith(Parameterized.class)
public class AsnCcpNewRouteUpdateRequestCodecTest extends AbstractAsnCodecTest<CcpRouteUpdateRequest> {

  /**
   * Construct an instance of this parameterized test with the supplied inputs.
   *
   * @param expectedCcpRouteUpdateRequest
   * @param asn1OerBytes                  The expected value, in binary, of the supplied {@code expectedPayloadLength}.
   */
  public AsnCcpNewRouteUpdateRequestCodecTest(
    final CcpRouteUpdateRequest expectedCcpRouteUpdateRequest, final byte[] asn1OerBytes
  ) {
    super(expectedCcpRouteUpdateRequest, asn1OerBytes, CcpRouteUpdateRequest.class);
  }

  /**
   * The data for this test...
   */
  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        // [ccp_control_request][byte_values]

        // 0 - JS Compatibility (simple request)
        {ImmutableCcpRouteUpdateRequest.builder()
          .routingTableId(RoutingTableId.of(UUID.fromString("21e55f8e-abcd-4e97-9ab9-bf0ff00a224c")))
          .currentEpochIndex(52)
          .fromEpochIndex(52)
          .toEpochIndex(52)
          .holdDownTime(30000)
          .speaker(InterledgerAddress.of("example.alice"))
          // No new nor withdrawn routes...
          .build(),
          BaseEncoding.base16().decode(
            "21E55F8EABCD4E979AB9BF0FF00A224C000000340000003400000034000075300D6578616D706C652E616C69636501000100")},

        {ImmutableCcpRouteUpdateRequest.builder()
          .routingTableId(RoutingTableId.of(UUID.fromString("21e55f8e-abcd-4e97-9ab9-bf0ff00a224c")))
          .currentEpochIndex(52)
          .fromEpochIndex(52)
          .toEpochIndex(52)
          .holdDownTime(30000)
          .speaker(InterledgerAddress.of("example.alice"))
          .withdrawnRoutePrefixes(
            Lists.newArrayList(
              ImmutableCcpWithdrawnRoute.builder().prefix(InterledgerAddressPrefix.of("example.prefix3")).build(),
              ImmutableCcpWithdrawnRoute.builder().prefix(InterledgerAddressPrefix.of("example.prefix4")).build()
            )
          )
          .build(),
          BaseEncoding.base16().decode(
            "21E55F8EABCD4E979AB9BF0FF00A224C000000340000003400000034000075300D6578616D706C652E616C696365010001020F6578616D706C652E707265666978330F6578616D706C652E70726566697834")},

        //0 - JS Compatibility(complex request)
        {ImmutableCcpRouteUpdateRequest.builder()
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
              ImmutableCcpWithdrawnRoute.builder().prefix(InterledgerAddressPrefix.of("example.prefix3")).build(),
              ImmutableCcpWithdrawnRoute.builder().prefix(InterledgerAddressPrefix.of("example.prefix4")).build()
            )
          )
          .build(),
          BaseEncoding.base16().decode(
            "BFFBF6AD0DDC4D3BA1E5B4F0537365BD000000340000002E00000032000075300D6578616D706C652E616C69636501020F6578616D706C652E7072656669783101010F6578616D706C652E707265666978317A6C7D85867C46A2FABFAD1AFA7A4A5E229CE574FCCE63F5EDEEDFC03F8468EA01000F6578616D706C652E707265666978320102126578616D706C652E636F6E6E6563746F72310F6578616D706C652E707265666978322B08E53FBCC17C5F1BD54AE0D9AD7BA39A5F9A7B126CA9B5C0945609A35324CC01025000000B68656C6C6F20776F726C64E0000104A0A0A0A001020F6578616D706C652E707265666978330F6578616D706C652E70726566697834")},

      }
    );
  }

}