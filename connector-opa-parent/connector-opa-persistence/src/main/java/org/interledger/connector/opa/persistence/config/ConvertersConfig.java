package org.interledger.connector.opa.persistence.config;

import org.interledger.connector.opa.persistence.converters.InvoiceEntityConverter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConvertersConfig {

  @Bean
  InvoiceEntityConverter invoiceEntityConverter() {
    return new InvoiceEntityConverter();
  }

}
