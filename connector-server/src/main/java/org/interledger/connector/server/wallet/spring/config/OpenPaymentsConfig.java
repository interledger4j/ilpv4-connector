package org.interledger.connector.server.wallet.spring.config;

import static org.interledger.connector.core.ConfigConstants.SPSP__URL_PATH;

import org.interledger.connector.opa.InvoiceService;
import org.interledger.connector.opa.OpaPaymentService;
import org.interledger.connector.persistence.repositories.AccountSettingsRepository;
import org.interledger.connector.persistence.repositories.InvoicesRepository;
import org.interledger.connector.wallet.IlpInvoiceService;
import org.interledger.connector.opa.model.OpenPaymentsSettings;
import org.interledger.connector.settings.properties.OpenPaymentsSettingsFromPropertyFile;
import org.interledger.connector.settings.properties.converters.HttpUrlPropertyConverter;
import org.interledger.connector.wallet.IlpOpaPaymentService;
import org.interledger.connector.wallet.OpenPaymentsClient;
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

import java.util.concurrent.ExecutorService;
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
    InvoicesRepository invoicesRepository,
    ConversionService conversionService
  ) {
    return new IlpInvoiceService(
      openPaymentsSettingsSupplier,
      paymentPointerResolver,
      opaUrlPath,
      invoicesRepository,
      conversionService);
  }

  @Bean
  public OpaPaymentService ilpOpaPaymentService(final PaymentPointerResolver paymentPointerResolver,
                                                final AccountSettingsRepository accountSettingsRepository,
                                                final OpenPaymentsClient openPaymentsClient,
                                                final OkHttpClient okHttpClient,
                                                final ObjectMapper objectMapper,
                                                @Value("${interledger.connector.connector-url}") final HttpUrl connectorUrl,
                                                @Value("${interledger.connector.nodeIlpAddress}") final InterledgerAddressPrefix opaAddressPrefix) { // FIXME: Is this the right value, or should we have a separate opa address prefix config value?
    return new IlpOpaPaymentService(paymentPointerResolver, accountSettingsRepository, openPaymentsClient, okHttpClient, objectMapper, connectorUrl, opaAddressPrefix);
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
