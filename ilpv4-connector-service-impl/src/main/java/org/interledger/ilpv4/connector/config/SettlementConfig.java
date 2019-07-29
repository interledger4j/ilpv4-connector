package org.interledger.ilpv4.connector.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.sappenin.interledger.ilpv4.connector.balances.BalanceTracker;
import com.sappenin.interledger.ilpv4.connector.links.LinkManager;
import com.sappenin.interledger.ilpv4.connector.settlement.SettlementEngineClient;
import com.sappenin.interledger.ilpv4.connector.settlement.SettlementService;
import okhttp3.HttpUrl;
import org.interledger.ilpv4.connector.persistence.repositories.AccountSettingsRepository;
import org.interledger.ilpv4.connector.settlement.DefaultSettlementService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestTemplate;

import static org.interledger.ilpv4.connector.config.SettlementEngineClientConfig.SETTLEMENT_ENGINE_CLIENT;

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
    @Qualifier(SETTLEMENT_ENGINE_CLIENT) RestTemplate restTemplate,
    Cache<HttpUrl, SettlementEngineClient> settlementEngineClientCache
  ) {
    return new DefaultSettlementService(balanceTracker, linkManager, accountSettingsRepository,
      restTemplate, settlementEngineClientCache);
  }

}
