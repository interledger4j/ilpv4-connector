package org.interledger.connector.server.spring.controllers.converters;

import org.interledger.connector.accounts.AccountRateLimitSettings;
import org.interledger.connector.persistence.entities.AccountRateLimitSettingsEntity;
import org.springframework.core.convert.converter.Converter;

/**
 * A converter from {@link AccountRateLimitSettingsEntity} to {@link AccountRateLimitSettings}.
 */
public class RateLimitSettingsConverter implements Converter<AccountRateLimitSettingsEntity, AccountRateLimitSettings> {

  @Override
  public AccountRateLimitSettings convert(final AccountRateLimitSettingsEntity entity) {
    return AccountRateLimitSettings.builder()
      .maxPacketsPerSecond(entity.getMaxPacketsPerSecond())
      .build();
  }
}
