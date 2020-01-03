package org.interledger.connector.server.spring.auth.ilpoverhttp;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.hash.HashCode;
import org.junit.Test;

public class BearerAuthenticationTest {

  public static final HashCode HASH_CODE = HashCode.fromString("ab");
  public static final byte[] BEARER_TOKEN = new byte[32];
  public static final String ACCOUNT_ID = "BOB";

  private static final BearerAuthentication AUTH = BearerAuthentication.builder()
      .principal(ACCOUNT_ID)
      .bearerToken(BEARER_TOKEN)
      .hmacSha256(HASH_CODE)
      .build();

  @Test
  public void getPrincipal() {
    assertThat(AUTH.getPrincipal()).isEqualTo(ACCOUNT_ID);
  }

  @Test
  public void isAuthenticated() {
    assertThat(AUTH.isAuthenticated()).isFalse();
  }

  @Test
  public void getAuthorities() {
    assertThat(AUTH.getAuthorities()).isEmpty();
  }

  @Test
  public void getCredentials() {
    assertThat(AUTH.getCredentials()).isEqualTo(BEARER_TOKEN);
  }

  @Test
  public void getDetails() {
    assertThat(AUTH.getDetails()).isNull();
  }

  @Test
  public void getName() {
    assertThat(AUTH.getName()).isEqualTo(ACCOUNT_ID);
  }
}
