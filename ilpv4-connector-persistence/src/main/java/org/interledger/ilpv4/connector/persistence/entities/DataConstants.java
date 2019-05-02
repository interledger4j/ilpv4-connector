package org.interledger.ilpv4.connector.persistence.entities;

public interface DataConstants {

  interface TableNames {
    String ACCOUNT_SETTINGS = "ACCOUNT_SETTINGS";
  }

  interface ColumnNames {
    String ACCOUNT_RELATIONSHIP = "ACCOUNT_RELATIONSHIP";
  }

  interface IndexNames {
    String ACCT_REL_IDX = "ACCT_REL_IDX";
  }

}
