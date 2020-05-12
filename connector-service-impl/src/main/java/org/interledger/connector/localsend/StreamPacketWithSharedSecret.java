package org.interledger.connector.localsend;

import org.interledger.core.SharedSecret;
import org.interledger.stream.StreamPacket;

import org.immutables.value.Value;

@Value.Immutable
public interface StreamPacketWithSharedSecret extends StreamPacket {

  static ImmutableStreamPacketWithSharedSecret.Builder builder() {
    return ImmutableStreamPacketWithSharedSecret.builder();
  }

  SharedSecret sharedSecret();
  
}
