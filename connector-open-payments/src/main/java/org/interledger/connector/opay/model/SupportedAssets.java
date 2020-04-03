package org.interledger.connector.opay.model;

public final class SupportedAssets {

  public static final SupportedAsset XRP = SupportedAsset.builder("XRP", (short) 9).build();
  public static final SupportedAsset USD = SupportedAsset.builder("USD", (short) 0).build();
  public static final SupportedAsset USD_CENTS = SupportedAsset.builder("USD", (short) 2).build();
  public static final SupportedAsset EUR = SupportedAsset.builder("EUR", (short) 0).build();
  public static final SupportedAsset EUR_CENTS = SupportedAsset.builder("EUR", (short) 2).build();
}
