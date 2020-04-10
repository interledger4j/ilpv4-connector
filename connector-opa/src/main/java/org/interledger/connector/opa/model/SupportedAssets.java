package org.interledger.connector.opa.model;

/**
 * A collection of common {@link SupportedAsset}s.
 */
public final class SupportedAssets {
  public static final SupportedAsset XRP = new SupportedAsset("XRP", (short) 9);
  public static final SupportedAsset USD = new SupportedAsset("USD", (short) 0);
  public static final SupportedAsset USD_CENTS = new SupportedAsset("USD", (short) 2);
  public static final SupportedAsset EUR = new SupportedAsset("EUR", (short) 0);
  public static final SupportedAsset EUR_CENTS = new SupportedAsset("EUR",( short) 2);
}