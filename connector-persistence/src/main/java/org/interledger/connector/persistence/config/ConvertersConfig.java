package org.interledger.connector.persistence.config;

import org.interledger.connector.persistence.converters.AccessTokenEntityConverter;
import org.interledger.connector.persistence.converters.AccountBalanceSettingsEntityConverter;
import org.interledger.connector.persistence.converters.AccountSettingsEntityConverter;
import org.interledger.connector.persistence.converters.FxRateOverridesEntityConverter;
import org.interledger.connector.persistence.converters.RateLimitSettingsEntityConverter;
import org.interledger.connector.persistence.converters.SettlementEngineDetailsEntityConverter;
import org.interledger.connector.persistence.converters.StaticRouteEntityConverter;
import org.interledger.connector.persistence.converters.TransactionFromEntityConverter;
import org.interledger.connector.persistence.converters.TransactionToEntityConverter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConvertersConfig {

  @Bean
  RateLimitSettingsEntityConverter rateLimitSettingsConverter() {
    return new RateLimitSettingsEntityConverter();
  }

  @Bean
  AccountBalanceSettingsEntityConverter accountBalanceSettingsConverter() {
    return new AccountBalanceSettingsEntityConverter();
  }

  @Bean
  SettlementEngineDetailsEntityConverter settlementEngineDetailsConverter() {
    return new SettlementEngineDetailsEntityConverter();
  }

  @Bean
  AccountSettingsEntityConverter accountSettingsConverter(
  ) {
    return new AccountSettingsEntityConverter(
      rateLimitSettingsConverter(), accountBalanceSettingsConverter(), settlementEngineDetailsConverter()
    );
  }

  @Bean
  FxRateOverridesEntityConverter fxRateOverrideEntityConverter() {
    return new FxRateOverridesEntityConverter();
  }

  @Bean
  StaticRouteEntityConverter staticRouteEntityConverter() {
    return new StaticRouteEntityConverter();
  }

  @Bean
  AccessTokenEntityConverter accessTokenEntityConverter() {
    return new AccessTokenEntityConverter();
  }

  @Bean
  TransactionFromEntityConverter transactionFromEntityConverter() {
    return new TransactionFromEntityConverter();
  }

  @Bean
  TransactionToEntityConverter transactionToEntityConverter() {
    return new TransactionToEntityConverter();
  }

}
