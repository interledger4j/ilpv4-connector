package org.interledger.connector.server.spring.auth.ilpoverhttp;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.accounts.AccountId;

import org.junit.Test;

public class SimpleCredentialsTest {

  private static final AccountId ACCOUNT_ID = AccountId.of("bob");
  private static final byte[] BEARER_TOKEN = new byte[32];

  private static final SimpleCredentials AUTH = SimpleCredentials.builder()
      .principal(ACCOUNT_ID)
      .authToken(BEARER_TOKEN)
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
    assertThat(AUTH.getName()).isEqualTo(ACCOUNT_ID.value());
  }
}
