package org.interledger.connector.opa.model;

public class OpenPaymentsPathConstants {
  private static final String SLASH = "/";

  public static final String AUTHORIZE = "authorize";
  public static final String SLASH_AUTHORIZE = SLASH + AUTHORIZE;

  public static final String TOKEN = "token";
  public static final String SLASH_TOKEN = SLASH + TOKEN;

  public static final String INVOICES = "invoices";
  public static final String SLASH_INVOICES = SLASH + INVOICES;

  public static final String INVOICE_ID = "invoiceId";
  public static final String INVOICE_ID_PARAM = "{" + INVOICE_ID + ":.+}";
  public static final String SLASH_INVOICE_ID = "/" + INVOICE_ID_PARAM;

  public static final String MANDATES = "mandates";
  public static final String SLASH_MANDATES = SLASH + MANDATES;

  public static final String ACCOUNTS = "accounts";
  public static final String SLASH_ACCOUNTS = SLASH + ACCOUNTS;

  public static final String OPA = "opa";
  public static final String SLASH_OPA = SLASH + OPA;

  public static final String PAY = "pay";
  public static final String SLASH_PAY = SLASH + PAY;

  public static final String ILP = "ilp";
  public static final String SLASH_ILP = SLASH + ILP;

  public static final String ACCOUNT_ID = "accountId";
  public static final String ACCOUNT_ID_PARAM = "{" + ACCOUNT_ID + ":.+}";
  public static final String SLASH_ACCOUNT_ID = "/" + ACCOUNT_ID_PARAM;

  public static final String SYNC = "sync";
  public static final String SLASH_SYNC = SLASH + SYNC;

  public static final String INVOICES_BASE = /*SLASH_ACCOUNTS + */SLASH_ACCOUNT_ID + SLASH_INVOICES;
  public static final String INVOICES_WITH_ID = INVOICES_BASE + SLASH_INVOICE_ID;
  public static final String SYNC_INVOICE = INVOICES_BASE + SLASH_SYNC;
  public static final String PAY_INVOICE = SLASH_ACCOUNTS + INVOICES_WITH_ID + SLASH_PAY;
}
