package org.interledger.connector.ccp.codecs;

import com.google.common.collect.Lists;
import org.interledger.connector.ccp.CcpRouteUpdateRequest;
import org.interledger.connector.ccp.ImmutableCcpRouteUpdateRequest;
import org.interledger.connector.routing.RoutingTableId;
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
      new AsnSequenceOfSequenceCodec(Lists::newArrayList, AsnCcpRouteCodec::new), // new routes
      new AsnSequenceOfSequenceCodec(Lists::newArrayList, AsnCcpWithdrawnRouteCodec::new) // withdrawn routes
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
      .routingTableId(RoutingTableId.of(getValueAt(0)))
      .currentEpochIndex(((Long) getValueAt(1)).intValue())
      .fromEpochIndex(((Long) getValueAt(2)).intValue())
      .toEpochIndex(((Long) getValueAt(3)).intValue())
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

    setValueAt(0, value.routingTableId().value());
    setValueAt(1, new Long(value.currentEpochIndex()));
    setValueAt(2, new Long(value.fromEpochIndex()));
    setValueAt(3, new Long(value.toEpochIndex()));
    setValueAt(4, new Long(value.holdDownTime()));
    setValueAt(5, value.speaker());
    setValueAt(6, value.newRoutes());
    setValueAt(7, value.withdrawnRoutePrefixes());
  }
}
