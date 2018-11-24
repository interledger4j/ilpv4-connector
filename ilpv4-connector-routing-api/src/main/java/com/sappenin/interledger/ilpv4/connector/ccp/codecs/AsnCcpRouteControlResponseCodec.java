package com.sappenin.interledger.ilpv4.connector.ccp.codecs;

import com.sappenin.interledger.ilpv4.connector.ccp.CcpRouteControlResponse;
import com.sappenin.interledger.ilpv4.connector.ccp.ImmutableCcpRouteControlResponse;
import org.interledger.encoding.asn.codecs.AsnSequenceCodec;

import java.util.Objects;

/**
 * A Codec instances of {@link CcpRouteControlResponse} to and from ASN.1 OER.
 */
public class AsnCcpRouteControlResponseCodec extends AsnSequenceCodec<CcpRouteControlResponse> {

  private static final CcpRouteControlResponse RESPONSE = ImmutableCcpRouteControlResponse.builder().build();

  /**
   * Default constructor.
   */
  public AsnCcpRouteControlResponseCodec() {
    super();
  }

  /**
   * Decode and return the value read into the codec during serialization.
   *
   * @return the decoded object
   */
  @Override
  public CcpRouteControlResponse decode() {
    return RESPONSE;
  }

  /**
   * Encode the provided {@link CcpRouteControlResponse} into ASN.1 OER bytes.
   *
   * @param value the {@link CcpRouteControlResponse} to encode.
   */
  @Override
  public void encode(final CcpRouteControlResponse value) {
    Objects.requireNonNull(value);
  }
}
