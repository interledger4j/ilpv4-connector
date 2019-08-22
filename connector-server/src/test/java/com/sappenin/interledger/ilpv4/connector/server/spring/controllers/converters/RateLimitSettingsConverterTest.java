package com.sappenin.interledger.ilpv4.connector.server.spring.controllers.converters;

import org.interledger.connector.accounts.AccountRateLimitSettings;
import org.interledger.ilpv4.connector.persistence.entities.AccountRateLimitSettingsEntity;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit tests for {@link RateLimitSettingsConverter}.
 */
public class RateLimitSettingsConverterTest {

  private RateLimitSettingsConverter converter;

  @Before
  public void setUp() {
    this.converter = new RateLimitSettingsConverter();
  }

  @Test
  public void convert() {
    final AccountRateLimitSettings rateLimitSettings = AccountRateLimitSettings.builder()
      .maxPacketsPerSecond(2)
      .build();
    final AccountRateLimitSettingsEntity entity = new AccountRateLimitSettingsEntity(rateLimitSettings);

    AccountRateLimitSettings actual = converter.convert(entity);

    assertThat(actual.getMaxPacketsPerSecond(), is(rateLimitSettings.getMaxPacketsPerSecond()));
  }
}
