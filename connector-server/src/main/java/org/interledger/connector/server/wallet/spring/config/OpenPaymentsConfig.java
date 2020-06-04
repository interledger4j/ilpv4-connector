package org.interledger.connector.server.wallet.spring.config;

import static org.interledger.connector.core.ConfigConstants.ENABLED_PROTOCOLS;
import static org.interledger.connector.core.ConfigConstants.OPEN_PAYMENTS_ENABLED;
import static org.interledger.connector.core.ConfigConstants.SPSP__URL_PATH;
import static org.interledger.connector.core.ConfigConstants.TRUE;

import org.interledger.connector.accounts.sub.LocalDestinationAddressUtils;
import org.interledger.connector.payments.SendPaymentService;
import org.interledger.connector.payments.StreamPayment;
import org.interledger.connector.persistence.repositories.InvoicesRepository;
import org.interledger.connector.persistence.repositories.PaymentsRepository;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.openpayments.IlpInvoiceService;
import org.interledger.openpayments.IlpPaymentDetails;
import org.interledger.openpayments.IlpPaymentSystemFacade;
import org.interledger.openpayments.InvoiceFactory;
import org.interledger.openpayments.PayIdResolver;
import org.interledger.openpayments.XrpPaymentDetails;
import org.interledger.openpayments.XrplInvoiceService;
import org.interledger.openpayments.config.OpenPaymentsSettings;
import org.interledger.openpayments.xrpl.XrplTransaction;
import org.interledger.spsp.PaymentPointerResolver;
import org.interledger.stream.receiver.ServerSecretSupplier;
import org.interledger.stream.receiver.StreamConnectionGenerator;

import com.google.common.eventbus.EventBus;
import org.interleger.openpayments.InvoiceService;
import org.interleger.openpayments.PaymentSystemFacade;
import org.interleger.openpayments.client.OpenPaymentsProxyClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;

import java.util.Optional;
import java.util.function.Supplier;

@Configuration
@ConditionalOnProperty(prefix = ENABLED_PROTOCOLS, name = OPEN_PAYMENTS_ENABLED, havingValue = TRUE)
//@EnableConfigurationProperties(OpenPaymentsSettingsFromPropertyFile.class)
@ComponentScan(basePackages = {
  "org.interledger.connector.server.wallet.controllers", // For Wallet
})
public class OpenPaymentsConfig {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  public static final String OPA_ILP = "ILP";
  public static final String XRP = "PAY_ID";

  @Bean
  public Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier(Supplier<ConnectorSettings> connectorSettings) {
    return () -> connectorSettings.get().openPayments().orElseThrow(
      () -> new IllegalStateException("missing open payments config"));
  }

  @Bean
  public InvoiceService<StreamPayment, IlpPaymentDetails> ilpInvoiceService(
    InvoicesRepository invoicesRepository,
    PaymentsRepository paymentsRepository,
    ConversionService conversionService,
    InvoiceFactory invoiceFactory,
    OpenPaymentsProxyClient openPaymentsProxyClient,
    Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier,
    PaymentSystemFacade<StreamPayment, IlpPaymentDetails> ilpPaymentSystemFacade,
    EventBus eventBus) {
    return new IlpInvoiceService(
      invoicesRepository,
      paymentsRepository,
      conversionService,
      invoiceFactory,
      openPaymentsProxyClient,
      openPaymentsSettingsSupplier,
      ilpPaymentSystemFacade,
      eventBus);
  }

  @Bean
  public InvoiceService<XrplTransaction, XrpPaymentDetails> xrpInvoiceService(
    InvoicesRepository invoicesRepository,
    PaymentsRepository paymentsRepository,
    ConversionService conversionService,
    InvoiceFactory invoiceFactory,
    OpenPaymentsProxyClient openPaymentsProxyClient,
    Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier,
    EventBus eventBus
  ) {
    return new XrplInvoiceService(
      invoicesRepository,
      paymentsRepository,
      conversionService,
      invoiceFactory,
      openPaymentsProxyClient,
      openPaymentsSettingsSupplier,
      eventBus
    );
  }

  @Bean
  @Qualifier(OPA_ILP)
  public PaymentSystemFacade<StreamPayment, IlpPaymentDetails> ilpPaymentSystemFacade(
    PaymentPointerResolver paymentPointerResolver,
    @Value("${" + SPSP__URL_PATH + ":}") final String opaUrlPath,
    StreamConnectionGenerator streamConnectionGenerator,
    ServerSecretSupplier serverSecretSupplier,
    SendPaymentService sendPaymentService,
    LocalDestinationAddressUtils localDestinationAddressUtils) {
    return new IlpPaymentSystemFacade(
      opaUrlPath,
      paymentPointerResolver,
      streamConnectionGenerator,
      serverSecretSupplier,
      sendPaymentService,
      localDestinationAddressUtils);
  }

  @Bean
  public OpenPaymentsProxyClient openPaymentsClient() {
    return OpenPaymentsProxyClient.construct();
  }

  @Bean
  public PaymentPointerResolver opaPaymentPointerResolver() {
    return PaymentPointerResolver.defaultResolver();
  }

  @Bean
  public InvoiceFactory invoiceFactory(
    PaymentPointerResolver paymentPointerResolver,
    PayIdResolver payIdPointerResolver,
    Supplier<ConnectorSettings> connectorSettings,
    Optional<String> opaUrlPath
  ) {
    return new InvoiceFactory(paymentPointerResolver, payIdPointerResolver, openPaymentsSettingsSupplier(connectorSettings), opaUrlPath);
  }

  @Bean
  public PayIdResolver payIdResolver() {
    return PayIdResolver.defaultPayIdResolver();
  }

}
