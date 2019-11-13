package org.interledger.connector.server.spring.auth.blast;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.accounts.AccountId;

import com.google.common.hash.HashCode;
import org.junit.Test;

public class AuthenticationDecisionTest {

  public static final AccountId ACCOUNT_ID = AccountId.of("bob");
  public static final HashCode HMAC = HashCode.fromString("ab");

  @Test
  public void isAuthenticated() {
    assertThat(newAuthenticated().isAuthenticated()).isTrue();
    assertThat(newUnauthenticated().isAuthenticated()).isFalse();
  }

  @Test
  public void getAuthorities() {
    assertThat(newAuthenticated().getAuthorities()).isEmpty();
    assertThat(newUnauthenticated().getAuthorities()).isEmpty();

  }

  @Test
  public void getCredentials() {
    assertThat(newAuthenticated().getCredentials()).isEqualTo(HMAC);
    assertThat(newUnauthenticated().getCredentials()).isEqualTo(HMAC);

  }

  @Test
  public void getDetails() {
    assertThat(newAuthenticated().getDetails()).isNull();
    assertThat(newUnauthenticated().getDetails()).isNull();
  }

  @Test
  public void getName() {
    assertThat(newAuthenticated().getName()).isEqualTo(ACCOUNT_ID.value());
    assertThat(newUnauthenticated().getName()).isNull();
  }

  private ImmutableAuthenticationDecision newAuthenticated() {
    return AuthenticationDecision.builder()
        .principal(ACCOUNT_ID)
        .credentialHmac(HashCode.fromString("ab"))
        .isAuthenticated(true)
        .build();
  }

  private ImmutableAuthenticationDecision newUnauthenticated() {
    return AuthenticationDecision.builder()
        .credentialHmac(HashCode.fromString("ab"))
        .isAuthenticated(false)
        .build();
  }
}