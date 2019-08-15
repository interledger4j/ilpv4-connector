package org.interledger.ilpv4.connector.config;

import com.sappenin.interledger.ilpv4.connector.balances.BalanceTracker;
import com.sappenin.interledger.ilpv4.connector.links.LinkManager;
import com.sappenin.interledger.ilpv4.connector.settlement.SettlementEngineClient;
import com.sappenin.interledger.ilpv4.connector.settlement.SettlementService;
import org.interledger.ilpv4.connector.persistence.repositories.AccountSettingsRepository;
import org.interledger.ilpv4.connector.settlement.DefaultSettlementService;
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
    SettlementEngineClient settlementEngineClient
  ) {
    return new DefaultSettlementService(balanceTracker, linkManager, accountSettingsRepository, settlementEngineClient);
  }

}
