package org.interledger.connector.server.spring.controllers;

/**
 * Constants for URL paths.
 */
public class PathConstants {

  public static final String SLASH_MANAGE = "/manage";
  public static final String SLASH_HEALTH = "/health";
  public static final String SLASH_INFO = "/info";

  public static final String SLASH = "/";
  public static final String ACCOUNT_ID = "accountId";
  public static final String SE_ACCOUNT_ID = "accountId";
  public static final String SLASH_ACCOUNTS = "/accounts";
  public static final String SLASH_ACCOUNT_ID = "/{" + ACCOUNT_ID + "}";
  public static final String SLASH_SE_ACCOUNT_ID = "/{" + SE_ACCOUNT_ID + "}";
  public static final String SLASH_SETTLEMENTS = "/settlements";
  public static final String SLASH_MESSAGES = "/messages";
  public static final String SLASH_ROUTES = "/routes";
  public static final String SLASH_ROUTES_STATIC = SLASH_ROUTES + "/static";
  public static final String SLASH_ACCOUNTS_ILP_PATH = SLASH_ACCOUNTS + SLASH_ACCOUNT_ID + "/ilp";
  public static final String SLASH_ACCOUNTS_BALANCE_PATH = SLASH_ACCOUNTS + SLASH_ACCOUNT_ID + "/balance";
  public static final String PREFIX = "prefix";
  public static final String SLASH_ROUTES_STATIC_PREFIX = SLASH_ROUTES_STATIC + "/{" + PREFIX + ":.+}";
}
