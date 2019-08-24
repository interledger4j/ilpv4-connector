package org.interledger.connector.persistence.converters;

import org.interledger.connector.accounts.AccountRateLimitSettings;
import org.interledger.connector.persistence.converters.RateLimitSettingsEntityConverter;
import org.interledger.connector.persistence.entities.AccountRateLimitSettingsEntity;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit tests for {@link RateLimitSettingsEntityConverter}.
 */
public class RateLimitSettingsEntityConverterTest {

  private RateLimitSettingsEntityConverter converter;

  @Before
  public void setUp() {
    this.converter = new RateLimitSettingsEntityConverter();
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
