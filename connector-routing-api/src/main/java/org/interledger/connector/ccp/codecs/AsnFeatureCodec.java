package org.interledger.connector.ccp.codecs;

import org.interledger.connector.ccp.CcpFeature;
import org.interledger.connector.ccp.ImmutableCcpFeature;
import org.interledger.encoding.asn.codecs.AsnIA5StringCodec;
import org.interledger.encoding.asn.codecs.AsnSequenceCodec;
import org.interledger.encoding.asn.codecs.AsnSizeConstraint;

public class AsnFeatureCodec extends AsnSequenceCodec<CcpFeature> {

  /**
   * Default constructor.
   */
  public AsnFeatureCodec() {
    super(
      new AsnIA5StringCodec(AsnSizeConstraint.UNCONSTRAINED)
    );
  }

  /**
   * Decode and return the value read into the codec during serialization.
   *
   * @return the decoded object
   */
  @Override
  public CcpFeature decode() {
    return ImmutableCcpFeature.builder()
      .value(getValueAt(0))
      .build();
  }

  /**
   * Encode the provided value into the codec to be written during serialization.
   *
   * @param value the value to encode
   */
  @Override
  public void encode(final CcpFeature value) {
    setValueAt(0, value.value());
  }
}
