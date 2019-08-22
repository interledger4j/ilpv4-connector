package org.interledger.connector.ccp.codecs;

import com.google.common.annotations.VisibleForTesting;
import org.interledger.connector.ccp.CcpRouteProperty;
import org.interledger.connector.ccp.ImmutableCcpRouteProperty;
import org.interledger.encoding.asn.codecs.AsnOctetStringCodec;
import org.interledger.encoding.asn.codecs.AsnSequenceCodec;
import org.interledger.encoding.asn.codecs.AsnUint16Codec;
import org.interledger.encoding.asn.codecs.AsnUint8Codec;

import java.util.Objects;

import static org.interledger.encoding.asn.codecs.AsnSizeConstraint.UNCONSTRAINED;

/**
 * Creates a sequence to encode a {@link CcpRouteProperty}.
 */
public class AsnCcpRoutePropertyCodec extends AsnSequenceCodec<CcpRouteProperty> {

  /**
   * Default constructor.
   */
  public AsnCcpRoutePropertyCodec() {
    super(
      new AsnUint8Codec(), // meta
      new AsnUint16Codec(), // id
      new AsnOctetStringCodec(UNCONSTRAINED) // prop.value
    );
  }

  /**
   * Decode and return the value read into the codec during serialization.
   *
   * @return the decoded object
   */
  @Override
  public CcpRouteProperty decode() {

    final short meta = getValueAt(0);

    final ImmutableCcpRouteProperty.Builder builder = this
      .decodeBooleanProperties(meta)
      .id(getValueAt(1));

    // Depending on the type of 'value', use one decoder or another.
    if (isUtf8(meta)) {
      builder.value(getValueAt(2));
    } else {
      builder.value(getValueAt(2));
    }

    return builder.build();
  }

  /**
   * Encode the provided value into the codec to be written during serialization.
   *
   * @param value the value to encode
   */
  @Override
  public void encode(final CcpRouteProperty value) {
    Objects.requireNonNull(value);
    setValueAt(0, encodeBooleanProperties(value)); // meta
    setValueAt(1, value.id()); // id

    if (value.utf8()) {
      setValueAt(2, value.value()); // value as bytes...
    } else {
      setValueAt(2, value.value()); // value as Octet String
    }
  }

  /**
   * Encode the binary propery values of {@code ccpRouteProperty} into a single 8-bit integer space.
   *
   * @param ccpRouteProperty An instance of {@link CcpRouteProperty} to encode boolean properties for.
   *
   * @return An integer that can be encoded into 8 bits.
   */
  protected short encodeBooleanProperties(final CcpRouteProperty ccpRouteProperty) {
    Objects.requireNonNull(ccpRouteProperty);

    short meta = 0;
    meta |= ccpRouteProperty.optional() ? 0x80 : 0;
    if (ccpRouteProperty.optional()) {
      meta |= ccpRouteProperty.transitive() ? 0x40 : 0;
      if (ccpRouteProperty.transitive()) {
        meta |= ccpRouteProperty.partial() ? 0x20 : 0;
      }
    } else {
      meta |= 0x40;
    }
    meta |= ccpRouteProperty.utf8() ? 0x10 : 0;

    return meta;
  }

  @VisibleForTesting
  protected ImmutableCcpRouteProperty.Builder decodeBooleanProperties(final short encodedMetaData) {
    return ImmutableCcpRouteProperty.builder()
      .optional((encodedMetaData & 0x80) == 128)
      .transitive((encodedMetaData & 0x40) == 64)
      .partial((encodedMetaData & 0x20) == 32)
      .utf8(isUtf8(encodedMetaData));
  }


  @VisibleForTesting
  protected boolean isUtf8(final short encodedMetaData) {
    return (encodedMetaData & 0x10) == 16;
  }

}
