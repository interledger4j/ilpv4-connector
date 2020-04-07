package org.interledger.connector.opa.controllers.constants;

public class PathConstants {
  public static final String SLASH = "/";
  public static final String OPEN_PAYMENTS_METADATA = "/.well-known/open-payments";

  public static final String AUTHORIZE = "authorize";
  public static final String SLASH_AUTHORIZE = SLASH + AUTHORIZE;

  public static final String TOKEN = "token";
  public static final String SLASH_TOKEN = SLASH + TOKEN;

  public static final String INVOICE = "invoice";
  public static final String SLASH_INVOICE = SLASH + INVOICE;

  public static final String MANDATE = "mandate";
  public static final String SLASH_MANDATE = SLASH + MANDATE;
}
