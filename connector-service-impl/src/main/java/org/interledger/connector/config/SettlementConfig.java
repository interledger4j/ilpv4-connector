package org.interledger.connector.config;

import com.google.common.eventbus.EventBus;
import org.interledger.connector.balances.BalanceTracker;
import org.interledger.connector.links.LinkManager;
import org.interledger.connector.settlement.DefaultSettlementService;
import org.interledger.connector.settlement.SettlementEngineClient;
import org.interledger.connector.settlement.SettlementService;
import org.interledger.connector.persistence.repositories.AccountSettingsRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration for supporting an ILP Settlement Engine.
 */
@Configuration
@Import({SettlementEngineClientConfig.class})
public class SettlementConfig {

  @Bean
  protected SettlementService settlementService(
    BalanceTracker balanceTracker,
    LinkManager linkManager,
    AccountSettingsRepository accountSettingsRepository,
    SettlementEngineClient settlementEngineClient,
    EventBus eventBus
  ) {
    return new DefaultSettlementService(
      balanceTracker, linkManager, accountSettingsRepository, settlementEngineClient, eventBus
    );
  }

}
