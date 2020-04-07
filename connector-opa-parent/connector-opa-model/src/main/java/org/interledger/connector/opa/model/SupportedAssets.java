package org.interledger.connector.opa.model;

public final class SupportedAssets {

    public static final SupportedAsset XRP = new SupportedAsset("XRP", (short) 9);
  public static final SupportedAsset USD = new SupportedAsset("USD", (short) 0);
  public static final SupportedAsset USD_CENTS = new SupportedAsset("USD", (short) 2);
  public static final SupportedAsset EUR = new SupportedAsset("EUR", (short) 0);
  public static final SupportedAsset EUR_CENTS = new SupportedAsset("EUR",( short) 2);
//  public static final SupportedAsset XRP = SupportedAsset.builder().assetCode("XRP").assetScale((short) 9).build();
//  public static final SupportedAsset USD = SupportedAsset.builder().assetCode("USD").assetScale((short) 0).build();
//  public static final SupportedAsset USD_CENTS = SupportedAsset.builder().assetCode("USD").assetScale((short) 2).build();
//  public static final SupportedAsset EUR = SupportedAsset.builder().assetCode("EUR").assetScale((short) 0).build();
//  public static final SupportedAsset EUR_CENTS = SupportedAsset.builder().assetCode("EUR").assetScale((short) 2).build();
}
