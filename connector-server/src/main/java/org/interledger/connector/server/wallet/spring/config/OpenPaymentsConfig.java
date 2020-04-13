package org.interledger.connector.server.wallet.spring.config;

import static org.interledger.connector.core.ConfigConstants.SPSP__URL_PATH;

import org.interledger.connector.opa.InvoiceService;
import org.interledger.connector.persistence.repositories.InvoicesRepository;
import org.interledger.connector.wallet.IlpInvoiceService;
import org.interledger.connector.opa.model.OpenPaymentsSettings;
import org.interledger.connector.settings.properties.OpenPaymentsSettingsFromPropertyFile;
import org.interledger.connector.settings.properties.converters.HttpUrlPropertyConverter;
import org.interledger.spsp.PaymentPointerResolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.function.Supplier;

@Configuration
@EnableConfigurationProperties(OpenPaymentsSettingsFromPropertyFile.class)
@ComponentScan(basePackages = {
  "org.interledger.connector.server.wallet.controllers", // For Wallet
})
public class OpenPaymentsConfig {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  private ApplicationContext applicationContext;

  @Bean
  public Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier() {
    return () -> applicationContext.getBean(OpenPaymentsSettings.class);
  }

  @Bean
  public HttpUrlPropertyConverter httpUrlPropertyConverter() {
    return new HttpUrlPropertyConverter();
  }

  @Bean
  public InvoiceService ilpInvoiceService(
    Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier,
    PaymentPointerResolver paymentPointerResolver,
    @Value("${" + SPSP__URL_PATH + ":}") final String opaUrlPath,
    InvoicesRepository invoicesRepository
  ) {
    return new IlpInvoiceService(
      openPaymentsSettingsSupplier,
      paymentPointerResolver,
      opaUrlPath,
      invoicesRepository
    );
  }

  @Bean
  public PaymentPointerResolver opaPaymentPointerResolver() {
    return PaymentPointerResolver.defaultResolver();
  }

}
