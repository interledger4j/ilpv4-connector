package org.interledger.connector.server.spring.auth.ilpoverhttp;

import okhttp3.HttpUrl;

/**
 * Utility methods related to JWKS url discovery
 */
public class JwksUtils {

  /**
   * Given an issuer url (e.g. https://foo.auth0.com) returns
   * the JWKS url (e.g. https://foo.auth0.com/.well-known/jwks.json)
   * @param issuerUrl issuer url
   * @return jkws url
   */
  public static HttpUrl getJwksUrl(HttpUrl issuerUrl) {
    HttpUrl.Builder builder = new HttpUrl.Builder()
      .scheme(issuerUrl.scheme())
      .host(issuerUrl.host())
      .port(issuerUrl.port());
    issuerUrl.pathSegments().forEach(builder::addPathSegment);
    return builder.addPathSegment(".well-known")
      .addPathSegment("jwks.json")
      .build();
  }

}
