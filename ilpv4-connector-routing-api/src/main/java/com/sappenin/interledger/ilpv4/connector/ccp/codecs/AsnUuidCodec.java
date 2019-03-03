package com.sappenin.interledger.ilpv4.connector.ccp.codecs;

import com.google.common.annotations.VisibleForTesting;
import org.interledger.encoding.asn.codecs.AsnOctetStringBasedObjectCodec;
import org.interledger.encoding.asn.codecs.AsnSizeConstraint;

import java.nio.ByteBuffer;
import java.util.UUID;

public class AsnUuidCodec extends AsnOctetStringBasedObjectCodec<UUID> {

  /**
   * Default constructor.
   */
  public AsnUuidCodec() {
    super(new AsnSizeConstraint(16,16));
  }

  /**
   * Convert a {@link UUID} into a byte array.
   *
   * @param uuid A {@link UUID}.
   *
   * @return A byte-array with the MostSignificant bits coming first (big-endian order).
   */
  @VisibleForTesting
  protected final static byte[] getBytesFromUUID(final UUID uuid) {
    final ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
    bb.putLong(uuid.getMostSignificantBits());
    bb.putLong(uuid.getLeastSignificantBits());

    return bb.array();
  }

  /**
   * Convert a big-endian byte-array into a {@link UUID}.
   *
   * @param bytes A byte array containing the bytes of a UUID.
   *
   * @return A {@link UUID} that matches the contents of {@code bytes}.
   */
  @VisibleForTesting
  protected final static UUID getUUIDFromBytes(final byte[] bytes) {
    final ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
    final Long high = byteBuffer.getLong();
    final Long low = byteBuffer.getLong();

    return new UUID(high, low);
  }

  /**
   * Decode and return the value read into the codec during serialization.
   *
   * @return the decoded object
   */
  @Override
  public UUID decode() {
    return getUUIDFromBytes(getBytes());
  }

  /**
   * Encode the provided value into the codec to be written during serialization.
   *
   * @param value the value to encode
   */
  @Override
  public void encode(final UUID value) {
    setBytes(getBytesFromUUID(value));
  }
}
