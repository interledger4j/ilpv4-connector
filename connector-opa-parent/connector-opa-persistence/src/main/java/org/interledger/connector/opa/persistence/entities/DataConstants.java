package org.interledger.connector.opa.persistence.entities;

public interface DataConstants {

  interface TableNames {
    String INVOICES = "INVOICES";
  }

  interface ColumnNames {
    // INVOICE ID
    String INVOICE_IDX_COLUMN_NAMES = "INVOICE_ID";
  }

  interface IndexNames {
    // INVOICES
    String INVOICES_ID_IDX = "INVOICES_ID_IDX";
  }

}
