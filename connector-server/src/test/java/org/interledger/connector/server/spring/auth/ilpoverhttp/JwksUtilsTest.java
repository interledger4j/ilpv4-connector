package org.interledger.connector.server.spring.auth.ilpoverhttp;

import static org.assertj.core.api.Assertions.assertThat;

import okhttp3.HttpUrl;
import org.junit.Test;

public class JwksUtilsTest {

  @Test
  public void getJwksUrlAtRootPath() {
    HttpUrl issuer = HttpUrl.parse("https://www.example.com");
    HttpUrl expectedJwks = HttpUrl.parse("https://www.example.com/.well-known/jwks.json");
    assertThat(JwksUtils.getJwksUrl(issuer)).isEqualTo(expectedJwks);
  }

  @Test
  public void getJwksUrlAtRootPathTrailingSlash() {
    HttpUrl issuer = HttpUrl.parse("https://www.example.com/");
    HttpUrl expectedJwks = HttpUrl.parse("https://www.example.com/.well-known/jwks.json");
    assertThat(JwksUtils.getJwksUrl(issuer)).isEqualTo(expectedJwks);
  }

  @Test
  public void getJwksUrlAtSubPath() {
    HttpUrl issuer = HttpUrl.parse("https://www.example.com/sub/path");
    HttpUrl expectedJwks = HttpUrl.parse("https://www.example.com/sub/path/.well-known/jwks.json");
    assertThat(JwksUtils.getJwksUrl(issuer)).isEqualTo(expectedJwks);
  }

  @Test
  public void getJwksUrlAtSubPathTrailingSlash() {
    HttpUrl issuer = HttpUrl.parse("https://www.example.com/sub/path/");
    HttpUrl expectedJwks = HttpUrl.parse("https://www.example.com/sub/path/.well-known/jwks.json");
    assertThat(JwksUtils.getJwksUrl(issuer)).isEqualTo(expectedJwks);
  }

}