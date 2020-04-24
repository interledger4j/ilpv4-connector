package org.interledger.connector.settings.properties;

public class OpenPaymentsPathConstants {
  private static final String SLASH = "/";
  public static final String OPEN_PAYMENTS_METADATA = "/.well-known/open-payments";

  public static final String AUTHORIZE = "authorize";
  public static final String SLASH_AUTHORIZE = SLASH + AUTHORIZE;

  public static final String TOKEN = "token";
  public static final String SLASH_TOKEN = SLASH + TOKEN;

  public static final String INVOICE = "invoice";
  public static final String SLASH_INVOICE = SLASH + INVOICE;

  public static final String MANDATE = "mandate";
  public static final String SLASH_MANDATE = SLASH + MANDATE;

  public static final String ACCOUNTS = "accounts";
  public static final String SLASH_ACCOUNTS = SLASH + ACCOUNTS;

  public static final String OPA = "opa";
  public static final String SLASH_OPA = SLASH + OPA;

  public static final String PAY = "pay";
  public static final String SLASH_PAY = SLASH + PAY;

  public static final String ILP = "ilp";
  public static final String SLASH_ILP = SLASH + ILP;

  public static final String ACCOUNT_ID = "accountId";
  public static final String SLASH_ACCOUNT_ID = "/{" + ACCOUNT_ID + ":.+}";
  public static final String SLASH_ACCOUNTS_OPA_PAY = SLASH_ACCOUNTS + SLASH_ACCOUNT_ID + SLASH_OPA + SLASH_PAY;
}
