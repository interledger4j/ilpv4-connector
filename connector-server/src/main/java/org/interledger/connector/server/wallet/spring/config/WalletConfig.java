package org.interledger.connector.server.wallet.spring.config;

import org.interledger.connector.opa.config.OpenPaymentsConfig;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan(basePackages = {
  "org.interledger.connector.server.wallet.controllers" // For Wallet
})
@Import(OpenPaymentsConfig.class)
public class WalletConfig {

}
