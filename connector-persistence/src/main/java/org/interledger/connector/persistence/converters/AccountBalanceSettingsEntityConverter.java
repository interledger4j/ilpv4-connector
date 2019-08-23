package org.interledger.connector.persistence.converters;

import org.interledger.connector.accounts.AccountBalanceSettings;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.persistence.entities.AccountBalanceSettingsEntity;
import org.interledger.connector.persistence.entities.AccountSettingsEntity;
import org.springframework.core.convert.converter.Converter;

import java.util.Objects;

/**
 * A converter from {@link AccountSettingsEntity} to {@link AccountSettings}.
 */
public class AccountBalanceSettingsEntityConverter implements
  Converter<AccountBalanceSettingsEntity, AccountBalanceSettings> {

  @Override
  public AccountBalanceSettings convert(final AccountBalanceSettingsEntity accountSettingsEntity) {
    Objects.requireNonNull(accountSettingsEntity);
    return AccountBalanceSettings.builder()
      .settleTo(accountSettingsEntity.getSettleTo())
      .settleThreshold(accountSettingsEntity.getSettleThreshold())
      .minBalance(accountSettingsEntity.getMinBalance())
      .build();
  }
}
