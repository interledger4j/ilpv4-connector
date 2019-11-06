package org.interledger.connector.persistence.entities;

public interface DataConstants {

  interface TableNames {
    String ACCOUNT_SETTINGS = "ACCOUNT_SETTINGS";
    String FX_RATE_OVERRIDES = "FX_RATE_OVERRIDES";
    String STATIC_ROUTES = "STATIC_ROUTES";
  }

  interface ColumnNames {
    // ACCOUNT_SETTINGS
    String ACCOUNT_RELATIONSHIP = "ACCOUNT_RELATIONSHIP";
    String SE_ACCOUNT_ID = "SE_ACCOUNT_ID";

    // FX_RATE_OVERRIDES
    String ASSET_CODE_IDX_COLUMNS = "ASSET_CODE_FROM,ASSET_CODE_TO";

    // STATIC_ROUTES
    String PREFIX = "NATURAL_ID";
  }

  interface IndexNames {
    // ACCOUNT_SETTINGS
    String ACCT_REL_IDX = "ACCT_REL_IDX";

    // FX_RATE_OVERRIDES
    String FX_RATE_OVERRIDES_IDX = "FX_RATE_OVERRIDES_IDX";

    // STATIC_ROUTES
    String STATIC_ROUTES_IDX = "STATIC_ROUTES_PREFIX_IDX";
  }

}
