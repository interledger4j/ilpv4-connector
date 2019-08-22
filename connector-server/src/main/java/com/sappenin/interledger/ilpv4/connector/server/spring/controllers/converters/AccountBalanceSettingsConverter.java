package com.sappenin.interledger.ilpv4.connector.server.spring.controllers.converters;

import org.interledger.connector.accounts.AccountBalanceSettings;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.ilpv4.connector.persistence.entities.AccountBalanceSettingsEntity;
import org.interledger.ilpv4.connector.persistence.entities.AccountSettingsEntity;
import org.springframework.core.convert.converter.Converter;

import java.util.Objects;

/**
 * A converter from {@link AccountSettingsEntity} to {@link AccountSettings}.
 */
public class AccountBalanceSettingsConverter implements
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
