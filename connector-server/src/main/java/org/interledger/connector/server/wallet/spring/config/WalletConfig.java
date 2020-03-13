package org.interledger.connector.server.wallet.spring.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import( {
  SpspConfig.class
})
@ComponentScan(basePackages = {
  "org.interledger.connector.server.wallet.controllers" // For Wallet
})
public class WalletConfig {

}
