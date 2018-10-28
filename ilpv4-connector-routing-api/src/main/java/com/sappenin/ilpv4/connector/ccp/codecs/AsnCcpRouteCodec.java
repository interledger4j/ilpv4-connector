package com.sappenin.ilpv4.connector.ccp.codecs;

import com.google.common.collect.Lists;
import com.sappenin.ilpv4.connector.ccp.CcpNewRoute;
import com.sappenin.ilpv4.connector.ccp.CcpRouteUpdateRequest;
import com.sappenin.ilpv4.connector.ccp.ImmutableCcpNewRoute;
import org.interledger.core.asn.codecs.AsnInterledgerAddressPrefixCodec;
import org.interledger.encoding.asn.codecs.AsnOctetStringCodec;
import org.interledger.encoding.asn.codecs.AsnSequenceCodec;
import org.interledger.encoding.asn.codecs.AsnSequenceOfSequenceCodec;
import org.interledger.encoding.asn.codecs.AsnSizeConstraint;

import java.util.ArrayList;
import java.util.Objects;

/**
 * A Codec instances of {@link CcpNewRoute} to and from ASN.1 OER.
 */
public class AsnCcpRouteCodec extends AsnSequenceCodec<CcpNewRoute> {

  /**
   * Default constructor.
   */
  public AsnCcpRouteCodec() {
    super(
      new AsnInterledgerAddressPrefixCodec(), // getRoute.prefix
      new AsnSequenceOfSequenceCodec(Lists::newArrayList, AsnCcpRoutePathPartCodec::new), // List of CcpRoutePaths
      new AsnOctetStringCodec(new AsnSizeConstraint(32, 32)), // auth must always be 32 bytes.
      new AsnSequenceOfSequenceCodec(ArrayList::new, AsnCcpRoutePropertyCodec::new) // Route Props
    );
  }

  /**
   * Decode and return the value read into the codec during serialization.
   *
   * @return the decoded object
   */
  @Override
  public CcpNewRoute decode() {
    return ImmutableCcpNewRoute.builder()
      .prefix(getValueAt(0))
      .path(getValueAt(1))
      .auth(getValueAt(2))
      .properties(getValueAt(3))
      .build();
  }

  /**
   * Encode the provided {@link CcpRouteUpdateRequest} into ASN.1 OER bytes.
   *
   * @param value the {@link CcpRouteUpdateRequest} to encode.
   */
  @Override
  public void encode(final CcpNewRoute value) {
    Objects.requireNonNull(value);

    setValueAt(0, value.prefix()); // getRoute.prefix
    setValueAt(1, value.path()); // Collection of pathParts
    setValueAt(2, value.auth()); // getRoute auth
    setValueAt(3, value.properties()); // Collection of getRoute properties
  }
}
