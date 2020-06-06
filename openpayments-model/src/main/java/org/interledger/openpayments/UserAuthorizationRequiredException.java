package org.interledger.openpayments;

import okhttp3.HttpUrl;

public class UserAuthorizationRequiredException extends Throwable {

  private final HttpUrl userAuthorizationUrl;

  public UserAuthorizationRequiredException(HttpUrl userAuthorizationUrl) {
    super("user must authorize request at " + userAuthorizationUrl.toString());
    this.userAuthorizationUrl = userAuthorizationUrl;
  }

  public HttpUrl getUserAuthorizationUrl() {
    return userAuthorizationUrl;
  }
}
