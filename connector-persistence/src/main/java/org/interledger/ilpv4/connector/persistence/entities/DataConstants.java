package org.interledger.ilpv4.connector.persistence.entities;

public interface DataConstants {

  interface TableNames {
    String ACCOUNT_SETTINGS = "ACCOUNT_SETTINGS";
  }

  interface ColumnNames {
    String ACCOUNT_RELATIONSHIP = "ACCOUNT_RELATIONSHIP";
    String SE_ACCOUNT_ID = "SE_ACCOUNT_ID";
  }

  interface IndexNames {
    String ACCT_REL_IDX = "ACCT_REL_IDX";
  }

}
