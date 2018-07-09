package com.sappenin.ilpv4.connector.ccp.codecs;

import com.sappenin.ilpv4.connector.ccp.Feature;
import com.sappenin.ilpv4.connector.ccp.ImmutableFeature;
import org.interledger.encoding.asn.codecs.AsnIA5StringCodec;
import org.interledger.encoding.asn.codecs.AsnSequenceCodec;
import org.interledger.encoding.asn.codecs.AsnSizeConstraint;

public class AsnFeatureCodec extends AsnSequenceCodec<Feature> {

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
  public Feature decode() {
    return ImmutableFeature.builder()
      .value(getValueAt(0))
      .build();
  }

  /**
   * Encode the provided value into the codec to be written during serialization.
   *
   * @param value the value to encode
   */
  @Override
  public void encode(final Feature value) {
    setValueAt(0, value.value());
  }
}
