package com.sappenin.ilpv4.connector.ccp;

/**
 * The state that the node wants its routing counterpart node to be in.
 */
public enum CcpSyncMode {

  MODE_IDLE((short) 0),
  MODE_SYNC((short) 1);

  private short value;

  CcpSyncMode(final short value) {
    this.value = value;
  }

  public static CcpSyncMode fromShort(final short value) {
    switch (value) {
      case 0: {
        return MODE_IDLE;
      }
      case 1: {
        return MODE_SYNC;
      }
      default: {
        throw new RuntimeException(String.format("Invalid CcpSyncMode Value: %d!", value));
      }
    }
  }

  public short getValue() {
    return value;
  }
}
