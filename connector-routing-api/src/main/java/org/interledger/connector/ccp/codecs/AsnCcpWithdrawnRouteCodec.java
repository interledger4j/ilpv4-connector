package org.interledger.connector.ccp.codecs;

import org.interledger.connector.ccp.CcpRouteUpdateRequest;
import org.interledger.connector.ccp.CcpWithdrawnRoute;
import org.interledger.connector.ccp.ImmutableCcpWithdrawnRoute;
import org.interledger.core.asn.codecs.AsnInterledgerAddressPrefixCodec;
import org.interledger.encoding.asn.codecs.AsnSequenceCodec;

import java.util.Objects;

/**
 * A Codec instances of {@link CcpWithdrawnRoute} to and from ASN.1 OER.
 */
public class AsnCcpWithdrawnRouteCodec extends AsnSequenceCodec<CcpWithdrawnRoute> {

  /**
   * Default constructor.
   */
  public AsnCcpWithdrawnRouteCodec() {
    super(
      new AsnInterledgerAddressPrefixCodec() // withdrawn getRoute's prefix
    );
  }

  /**
   * Decode and return the value read into the codec during serialization.
   *
   * @return the decoded object
   */
  @Override
  public CcpWithdrawnRoute decode() {
    return ImmutableCcpWithdrawnRoute.builder()
      .prefix(getValueAt(0))
      .build();
  }

  /**
   * Encode the provided {@link CcpRouteUpdateRequest} into ASN.1 OER bytes.
   *
   * @param value the {@link CcpRouteUpdateRequest} to encode.
   */
  @Override
  public void encode(final CcpWithdrawnRoute value) {
    Objects.requireNonNull(value);
    setValueAt(0, value.prefix()); // withdrawn getRoute.
  }
}
