package org.interledger.connector.server.wallet.spring.config;

import static org.interledger.connector.core.ConfigConstants.SPSP__URL_PATH;

import org.interledger.connector.opa.InvoiceService;
import org.interledger.connector.opa.PaymentDetailsService;
import org.interledger.connector.persistence.repositories.AccountSettingsRepository;
import org.interledger.connector.persistence.repositories.InvoicesRepository;
import org.interledger.connector.wallet.DefaultInvoiceService;
import org.interledger.connector.opa.model.OpenPaymentsSettings;
import org.interledger.connector.settings.properties.OpenPaymentsSettingsFromPropertyFile;
import org.interledger.connector.settings.properties.converters.HttpUrlPropertyConverter;
import org.interledger.connector.wallet.IlpPaymentDetailsService;
import org.interledger.connector.wallet.OpenPaymentsClient;
import org.interledger.connector.wallet.XrpPaymentDetailsService;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.spsp.PaymentPointerResolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;

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
  public InvoiceService defaultInvoiceService(
    InvoicesRepository invoicesRepository,
    ConversionService conversionService
  ) {
    return new DefaultInvoiceService(invoicesRepository, conversionService);
  }

  @Bean
  public PaymentDetailsService ilpPaymentDetailsService(
    Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier,
    PaymentPointerResolver paymentPointerResolver,
    @Value("${" + SPSP__URL_PATH + ":}") final String opaUrlPath
  ) {
    return new IlpPaymentDetailsService(openPaymentsSettingsSupplier, opaUrlPath, paymentPointerResolver);
  }

  @Bean
  public PaymentDetailsService payIdPaymentDetailsService() {
    return new XrpPaymentDetailsService();
  }
  
  @Bean
  public OpenPaymentsClient openPaymentsClient() {
    return OpenPaymentsClient.construct();
  }

  @Bean
  public PaymentPointerResolver opaPaymentPointerResolver() {
    return PaymentPointerResolver.defaultResolver();
  }

}
