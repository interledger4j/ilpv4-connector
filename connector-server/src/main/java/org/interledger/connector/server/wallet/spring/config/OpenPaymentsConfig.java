package org.interledger.connector.server.wallet.spring.config;

import static org.interledger.connector.core.ConfigConstants.SPSP__URL_PATH;

import org.interledger.connector.opa.InvoiceService;
import org.interledger.connector.opa.PaymentDetailsService;
import org.interledger.connector.opa.model.InvoiceFactory;
import org.interledger.connector.opa.model.OpenPaymentsSettings;
import org.interledger.connector.persistence.repositories.InvoicesRepository;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.connector.settings.properties.converters.HttpUrlPropertyConverter;
import org.interledger.connector.wallet.DefaultInvoiceService;
import org.interledger.connector.wallet.IlpPaymentDetailsService;
import org.interledger.connector.wallet.OpenPaymentsClient;
import org.interledger.connector.wallet.XrpPaymentDetailsService;
import org.interledger.spsp.PaymentPointerResolver;
import org.interledger.stream.receiver.ServerSecretSupplier;
import org.interledger.stream.receiver.StreamConnectionGenerator;

import io.xpring.common.XRPLNetwork;
import io.xpring.payid.PayIDClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;

import java.util.function.Supplier;

@Configuration
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
    return () -> connectorSettings.get().openPayments();
  }

  @Bean
  public HttpUrlPropertyConverter httpUrlPropertyConverter() {
    return new HttpUrlPropertyConverter();
  }

  @Bean
  public InvoiceService defaultInvoiceService(
    InvoicesRepository invoicesRepository,
    ConversionService conversionService,
    InvoiceFactory invoiceFactory,
    OpenPaymentsClient openPaymentsClient,
    Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier,
    PaymentDetailsService xrpPaymentDetailsService,
    PaymentDetailsService ilpPaymentDetailsService
  ) {
    return new DefaultInvoiceService(invoicesRepository, conversionService, invoiceFactory, openPaymentsClient, openPaymentsSettingsSupplier, xrpPaymentDetailsService, ilpPaymentDetailsService);
  }

  @Bean
  @Qualifier(OPA_ILP)
  public PaymentDetailsService ilpPaymentDetailsService(
    Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier,
    PaymentPointerResolver paymentPointerResolver,
    @Value("${" + SPSP__URL_PATH + ":}") final String opaUrlPath,
    StreamConnectionGenerator streamConnectionGenerator,
    ServerSecretSupplier serverSecretSupplier
  ) {
    return new IlpPaymentDetailsService(openPaymentsSettingsSupplier, opaUrlPath, paymentPointerResolver, streamConnectionGenerator, serverSecretSupplier);
  }

  @Bean
  @Qualifier(XRP)
  public PaymentDetailsService xrpPaymentDetailsService(PayIDClient payIDClient) {
    return new XrpPaymentDetailsService(payIDClient);
  }

  @Bean
  public PayIDClient payIDClient() {
    // TODO: make network configurable
    return new PayIDClient(XRPLNetwork.TEST);
  }

  @Bean
  public OpenPaymentsClient openPaymentsClient() {
    return OpenPaymentsClient.construct();
  }

  @Bean
  public PaymentPointerResolver opaPaymentPointerResolver() {
    return PaymentPointerResolver.defaultResolver();
  }

  @Bean
  public InvoiceFactory invoiceFactory(PaymentPointerResolver paymentPointerResolver, Supplier<ConnectorSettings> connectorSettings) {
    return new InvoiceFactory(paymentPointerResolver, openPaymentsSettingsSupplier(connectorSettings));
  }

}
