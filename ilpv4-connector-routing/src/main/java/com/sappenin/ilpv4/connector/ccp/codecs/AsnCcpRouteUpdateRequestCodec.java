package com.sappenin.ilpv4.connector.ccp.codecs;

import com.google.common.collect.Lists;
import com.sappenin.ilpv4.connector.ccp.CcpRouteUpdateRequest;
import com.sappenin.ilpv4.connector.ccp.ImmutableCcpRouteUpdateRequest;
import org.interledger.core.asn.codecs.AsnInterledgerAddressCodec;
import org.interledger.encoding.asn.codecs.AsnSequenceCodec;
import org.interledger.encoding.asn.codecs.AsnSequenceOfSequenceCodec;
import org.interledger.encoding.asn.codecs.AsnUint32Codec;

import java.util.Objects;

/**
 * A Codec instances of {@link CcpRouteUpdateRequest} to and from ASN.1 OER.
 */
public class AsnCcpRouteUpdateRequestCodec extends AsnSequenceCodec<CcpRouteUpdateRequest> {

  /**
   * Default constructor.
   */
  public AsnCcpRouteUpdateRequestCodec() {
    super(
      new AsnUuidCodec(), // RoutingTableId (UUID)
      new AsnUint32Codec(), // current epoch index
      new AsnUint32Codec(), // from epoch index
      new AsnUint32Codec(), // to epoch index
      new AsnUint32Codec(), // hold down time
      new AsnInterledgerAddressCodec(), // speaker
      new AsnSequenceOfSequenceCodec(Lists::newArrayList, AsnCcpRouteCodec::new), // routes
      new AsnSequenceOfSequenceCodec(Lists::newArrayList, AsnCcpWithdrawnRouteCodec::new) // request
      // .withdrawnRoutePrefixes
    );
  }

  /**
   * Decode and return the value read into the codec during serialization.
   *
   * @return the decoded object
   */
  @Override
  public CcpRouteUpdateRequest decode() {
    return ImmutableCcpRouteUpdateRequest.builder()
      .routingTableId(getValueAt(0))
      .currentEpochIndex(getValueAt(1))
      .fromEpochIndex(getValueAt(2))
      .toEpochIndex(getValueAt(3))
      .holdDownTime(getValueAt(4))
      .speaker(getValueAt(5))
      .newRoutes(getValueAt(6))
      .withdrawnRoutePrefixes(getValueAt(7))
      .build();
  }

  /**
   * Encode the provided {@link CcpRouteUpdateRequest} into ASN.1 OER bytes.
   *
   * @param value the {@link CcpRouteUpdateRequest} to encode.
   */
  @Override
  public void encode(final CcpRouteUpdateRequest value) {
    Objects.requireNonNull(value);

    setValueAt(0, value.routingTableId());
    setValueAt(1, value.currentEpochIndex());
    setValueAt(2, value.fromEpochIndex());
    setValueAt(3, value.toEpochIndex());
    setValueAt(4, value.holdDownTime());
    setValueAt(5, value.speaker());
    setValueAt(6, value.newRoutes());
    setValueAt(7, value.withdrawnRoutePrefixes());
  }
}
