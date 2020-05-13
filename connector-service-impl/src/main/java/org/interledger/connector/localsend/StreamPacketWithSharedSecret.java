package org.interledger.connector.localsend;

import org.interledger.core.SharedSecret;
import org.interledger.stream.StreamPacket;

import org.immutables.value.Value;

/**
 * Extensions of StreamPacket that additional carries a transitive property for the shared secret. Used by
 * local send logic in order to decrypt the stream packet on the fulfillment response.
 */
@Value.Immutable
public interface StreamPacketWithSharedSecret extends StreamPacket {

  static ImmutableStreamPacketWithSharedSecret.Builder builder() {
    return ImmutableStreamPacketWithSharedSecret.builder();
  }

  SharedSecret sharedSecret();
  
}
