package com.sappenin.ilpv4.connector.ccp.codecs;

import com.sappenin.ilpv4.connector.ccp.CcpRoutePathPart;
import com.sappenin.ilpv4.connector.ccp.ImmutableCcpRoutePathPart;
import org.interledger.core.asn.codecs.AsnInterledgerAddressCodec;
import org.interledger.encoding.asn.codecs.AsnSequenceCodec;

import java.util.Objects;

/**
 * An extension of {@link AsnSequenceCodec} for handling instances of {@link CcpRoutePathPart}.
 */
public class AsnCcpRoutePathPartCodec extends AsnSequenceCodec<CcpRoutePathPart> {

  /**
   * Default constructor.
   */
  public AsnCcpRoutePathPartCodec() {
    super(
      new AsnInterledgerAddressCodec()
    );
  }

  /**
   * Decode and return the value read into the codec during serialization.
   *
   * @return the decoded object
   */
  @Override
  public CcpRoutePathPart decode() {
    return ImmutableCcpRoutePathPart.builder()
      .routePathPart(getValueAt(0))
      .build();
  }

  /**
   * Encode the provided value into the codec to be written during serialization.
   *
   * @param value the value to encode
   */
  @Override
  public void encode(final CcpRoutePathPart value) {
    Objects.requireNonNull(value);
    setValueAt(0, value.routePathPart());
  }
}
