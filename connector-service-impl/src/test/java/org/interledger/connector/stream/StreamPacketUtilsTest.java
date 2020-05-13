package org.interledger.connector.stream;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerPacketType;
import org.interledger.stream.Denomination;
import org.interledger.stream.StreamPacket;
import org.interledger.stream.StreamPacketBuilder;
import org.interledger.stream.frames.ConnectionAssetDetailsFrame;
import org.interledger.stream.frames.ConnectionNewAddressFrame;
import org.interledger.stream.frames.ErrorCodes;
import org.interledger.stream.frames.StreamCloseFrame;
import org.interledger.stream.frames.StreamMoneyFrame;

import com.google.common.primitives.UnsignedLong;
import org.junit.Test;

public class StreamPacketUtilsTest {

  private static final Denomination DENOMINATION = Denomination.builder()
    .assetScale((short) 9)
    .assetCode("XRP")
    .build();

  private static final InterledgerAddress ADDRESS = InterledgerAddress.of("test.foo");

  private static final StreamPacket JUST_CONNECTION_ASSET_DETAILS_FRAME =
    newPacketBuilder().addFrames(assetDetailsFrame(DENOMINATION)).build();

  private static final StreamPacket JUST_MONEY_FRAME =
    newPacketBuilder().addFrames(moneyFrame()).build();

  private static final StreamPacket JUST_CLOSE_FRAME =
    newPacketBuilder().addFrames(closeFrame()).build();

  private static final StreamPacket JUST_CONNECTION_NEW_ADDRESS_FRAME =
    newPacketBuilder().addFrames(connectionNewAddressFrame(ADDRESS)).build();

  private static final StreamPacket ALL_THE_FRAMES =
    newPacketBuilder().addFrames(
      closeFrame(),
      assetDetailsFrame(DENOMINATION),
      moneyFrame(),
      connectionNewAddressFrame(ADDRESS)).build();

  private static final StreamPacket NO_FRAMES = newPacketBuilder().build();

  @Test
  public void getDenomination() {
    assertThat(StreamPacketUtils.getDenomination(JUST_CONNECTION_ASSET_DETAILS_FRAME)).hasValue(DENOMINATION);
    assertThat(StreamPacketUtils.getDenomination(ALL_THE_FRAMES)).hasValue(DENOMINATION);
    assertThat(StreamPacketUtils.getDenomination(JUST_MONEY_FRAME)).isEmpty();
    assertThat(StreamPacketUtils.getDenomination(NO_FRAMES)).isEmpty();
  }

  @Test
  public void hasCloseFrame() {
    assertThat(StreamPacketUtils.hasCloseFrame(JUST_CLOSE_FRAME)).isTrue();
    assertThat(StreamPacketUtils.hasCloseFrame(ALL_THE_FRAMES)).isTrue();
    assertThat(StreamPacketUtils.hasCloseFrame(JUST_MONEY_FRAME)).isFalse();
    assertThat(StreamPacketUtils.hasCloseFrame(NO_FRAMES)).isFalse();
  }

  @Test
  public void getSourceAddress() {
    assertThat(StreamPacketUtils.getSourceAddress(JUST_CONNECTION_NEW_ADDRESS_FRAME)).hasValue(ADDRESS);
    assertThat(StreamPacketUtils.getSourceAddress(ALL_THE_FRAMES)).hasValue(ADDRESS);
    assertThat(StreamPacketUtils.getSourceAddress(JUST_MONEY_FRAME)).isEmpty();
    assertThat(StreamPacketUtils.getSourceAddress(NO_FRAMES)).isEmpty();
  }

  private static StreamPacketBuilder newPacketBuilder() {
    return StreamPacket.builder()
      .prepareAmount(UnsignedLong.ONE)
      .sequence(UnsignedLong.ONE)
      .interledgerPacketType(InterledgerPacketType.PREPARE);
  }

  private static StreamMoneyFrame moneyFrame() {
    return StreamMoneyFrame.builder().shares(UnsignedLong.ONE)
      .streamId(UnsignedLong.ONE)
      .build();
  }

  private static ConnectionNewAddressFrame connectionNewAddressFrame(InterledgerAddress address) {
    return ConnectionNewAddressFrame.builder().sourceAddress(address)
      .build();
  }

  private static ConnectionAssetDetailsFrame assetDetailsFrame(Denomination denomination) {
    return ConnectionAssetDetailsFrame.builder()
      .sourceDenomination(denomination)
      .build();
  }

  private static StreamCloseFrame closeFrame() {
    return StreamCloseFrame.builder()
      .streamId(UnsignedLong.ONE)
      .errorCode(ErrorCodes.NoError)
      .build();
  }

}