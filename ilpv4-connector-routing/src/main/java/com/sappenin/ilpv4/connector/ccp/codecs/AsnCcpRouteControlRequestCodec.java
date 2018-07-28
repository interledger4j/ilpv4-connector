package com.sappenin.ilpv4.connector.ccp.codecs;

import com.google.common.collect.Lists;
import com.sappenin.ilpv4.connector.ccp.CcpMode;
import com.sappenin.ilpv4.connector.ccp.CcpRouteControlRequest;
import com.sappenin.ilpv4.connector.ccp.ImmutableCcpRouteControlRequest;
import org.interledger.encoding.asn.codecs.AsnSequenceCodec;
import org.interledger.encoding.asn.codecs.AsnSequenceOfSequenceCodec;
import org.interledger.encoding.asn.codecs.AsnUint32Codec;
import org.interledger.encoding.asn.codecs.AsnUint8Codec;

import java.util.Objects;

/**
 * A Codec instances of {@link CcpRouteControlRequest} to and from ASN.1 OER.
 */
public class AsnCcpRouteControlRequestCodec extends AsnSequenceCodec<CcpRouteControlRequest> {

  /**
   * Default constructor.
   */
  public AsnCcpRouteControlRequestCodec() {
    super(
      new AsnUint8Codec(), // Mode
      new AsnUuidCodec(), // RoutingTableId (UUID)
      new AsnUint32Codec(), // The epoch
      new AsnSequenceOfSequenceCodec(Lists::newArrayList, AsnFeatureCodec::new) // Feature List.
    );
  }

  /**
   * Decode and return the value read into the codec during serialization.
   *
   * @return the decoded object
   */
  @Override
  public CcpRouteControlRequest decode() {
    return ImmutableCcpRouteControlRequest.builder()
      .mode(CcpMode.fromInt(getValueAt(0)))
      .lastKnownRoutingTableId(getValueAt(1))
      .lastKnownEpoch(getValueAt(2))
      .features(getValueAt(3))
      .build();
  }

  /**
   * Encode the provided {@link CcpRouteControlRequest} into ASN.1 OER bytes.
   *
   * @param value the {@link CcpRouteControlRequest} to encode.
   */
  @Override
  public void encode(final CcpRouteControlRequest value) {
    Objects.requireNonNull(value);

    setValueAt(0, value.getMode().getValue());
    setValueAt(1, value.lastKnownRoutingTableId());
    setValueAt(2, value.lastKnownEpoch());
    setValueAt(3, value.features());
  }
}
