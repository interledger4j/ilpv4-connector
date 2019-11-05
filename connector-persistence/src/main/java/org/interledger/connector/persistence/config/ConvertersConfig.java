package org.interledger.connector.persistence.config;

import org.interledger.connector.persistence.converters.AccountBalanceSettingsEntityConverter;
import org.interledger.connector.persistence.converters.AccountSettingsEntityConverter;
import org.interledger.connector.persistence.converters.FxRateOverridesEntityConverter;
import org.interledger.connector.persistence.converters.RateLimitSettingsEntityConverter;
import org.interledger.connector.persistence.converters.SettlementEngineDetailsEntityConverter;
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

}
