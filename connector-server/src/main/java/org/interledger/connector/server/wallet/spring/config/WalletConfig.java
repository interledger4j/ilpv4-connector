package org.interledger.connector.server.wallet.spring.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {
  "org.interledger.connector.server.wallet.controllers" // For Wallet
})
public class WalletConfig {

}
