package com.sappenin.ilpv4.connector.ccp;

/**
 * Modes that a node can be in when supporting the CCP.
 */
public enum CcpMode {

  MODE_IDLE((short) 0),
  MODE_SYNC((short) 1);

  private short value;

  CcpMode(final short value) {
    this.value = value;
  }

  public static CcpMode fromShort(final short value) {
    switch (value) {
      case 0: {
        return MODE_IDLE;
      }
      case 1: {
        return MODE_SYNC;
      }
      default: {
        throw new RuntimeException(String.format("Invalid CcpMode Value: %d!", value));
      }
    }
  }

  public short getValue() {
    return value;
  }
}
