package org.interledger.connector.stream;

import org.interledger.core.InterledgerAddress;
import org.interledger.stream.Denomination;
import org.interledger.stream.StreamPacket;
import org.interledger.stream.frames.ConnectionAssetDetailsFrame;
import org.interledger.stream.frames.ConnectionNewAddressFrame;
import org.interledger.stream.frames.StreamFrame;
import org.interledger.stream.frames.StreamFrameType;

import com.google.common.collect.Sets;

import java.util.Optional;
import java.util.Set;

public class StreamPacketUtils {

  private static final Set<StreamFrameType> CLOSING_FRAMES = Sets.newHashSet(
    StreamFrameType.ConnectionClose,
    StreamFrameType.StreamClose
  );

  public static Optional<Denomination> getDenomination(StreamPacket streamPacket) {
    return streamPacket.frames().stream()
      .filter(frame -> frame.streamFrameType().equals(StreamFrameType.ConnectionAssetDetails))
      .map(frame -> ((ConnectionAssetDetailsFrame) frame))
      .map(ConnectionAssetDetailsFrame::sourceDenomination)
      .findFirst();
  }

  public static boolean hasCloseFrame(StreamPacket streamPacket) {
    return streamPacket.frames().stream()
      .map(StreamFrame::streamFrameType)
      .anyMatch(CLOSING_FRAMES::contains);
  }

  public static Optional<InterledgerAddress> getSourceAddress(StreamPacket streamPacket) {
    return streamPacket.frames().stream()
      .filter(frame -> frame.streamFrameType().equals(StreamFrameType.ConnectionNewAddress))
      .map(frame -> ((ConnectionNewAddressFrame) frame).sourceAddress())
      .filter(Optional::isPresent)
      .map(Optional::get)
      .findFirst();
  }

}
