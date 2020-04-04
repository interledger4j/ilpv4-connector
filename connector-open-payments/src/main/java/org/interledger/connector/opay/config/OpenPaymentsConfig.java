package org.interledger.connector.opay.config;

import org.interledger.connector.opay.model.OpenPaymentsMetadata;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.function.Supplier;

@Configuration
@EnableConfigurationProperties(OpenPaymentsMetadataFromPropertyFile.class)
@ComponentScan(basePackages = {"org.interledger.connector.opay"})
public class OpenPaymentsConfig {

  @Autowired
  private ApplicationContext applicationContext;

  @Bean
  Supplier<OpenPaymentsMetadata> openPaymentsMetadataSupplier() {
    return () -> applicationContext.getBean(OpenPaymentsMetadata.class);
  }
}
