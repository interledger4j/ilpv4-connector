package org.interledger.connector.ccp.codecs;

import com.google.common.collect.Lists;
import org.interledger.connector.ccp.CcpRouteControlRequest;
import org.interledger.connector.ccp.CcpSyncMode;
import org.interledger.connector.ccp.ImmutableCcpRouteControlRequest;
import org.interledger.connector.routing.RoutingTableId;
import org.interledger.encoding.asn.codecs.AsnSequenceCodec;
import org.interledger.encoding.asn.codecs.AsnSequenceOfSequenceCodec;
import org.interledger.encoding.asn.codecs.AsnUint32Codec;
import org.interledger.encoding.asn.codecs.AsnUint8Codec;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A Codec instances of {@link CcpRouteControlRequest} to and from ASN.1 OER.
 */
public class AsnCcpRouteControlRequestCodec extends AsnSequenceCodec<CcpRouteControlRequest> {

  // The default UUID if no routing table identifier exists.
  private static final UUID ZERO_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

  /**
   * Default constructor.
   */
  public AsnCcpRouteControlRequestCodec() {
    super(
      new AsnUint8Codec(), // Mode
      new AsnUuidCodec(), // RoutingTableId (UUID)
      new AsnUint32Codec(), // The epoch (32 bits wide).
      new AsnSequenceOfSequenceCodec(Lists::newArrayList, AsnFeatureCodec::new) // CcpFeature List.
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
      .mode(CcpSyncMode.fromShort(getValueAt(0)))
      // Treat the ZERO_UUID as being absent.
      .lastKnownRoutingTableId(
        Optional.ofNullable(ZERO_UUID.equals(getValueAt(1)) ? null : RoutingTableId.of(getValueAt(1)))
      )
      .lastKnownEpoch(((Long) getValueAt(2)).intValue())
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
    // We can't easily send an empty UUID, so we instead send the all-zero UUID.
    setValueAt(1, value.lastKnownRoutingTableId().map(RoutingTableId::value).orElse(ZERO_UUID));
    setValueAt(2, new Long(value.lastKnownEpoch()));
    setValueAt(3, value.features());
  }
}
