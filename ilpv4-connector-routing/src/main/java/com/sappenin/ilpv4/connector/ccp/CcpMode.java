package com.sappenin.ilpv4.connector.ccp;

/**
 * Modes that a node can be in when supporting the CCP.
 */
public enum CcpMode {

  MODE_IDLE(0),
  MODE_SYNC(1);

  private int value;

  CcpMode(final int value) {
    this.value = value;
  }

  public static CcpMode fromInt(final int value) {
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

  public int getValue() {
    return value;
  }
}
