package com.sappenin.interledger.ilpv4.connector.server.javamoney;

import com.sappenin.interledger.ilpv4.connector.server.ConnectorServerConfig;
import com.sappenin.interledger.ilpv4.connector.server.spring.settings.properties.ConnectorProperties;
import org.javamoney.moneta.spi.DefaultNumberValue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.money.convert.ConversionQueryBuilder;
import javax.money.convert.ExchangeRate;
import javax.money.convert.ExchangeRateProvider;
import javax.money.convert.RateType;
import java.math.BigInteger;

import static com.sappenin.interledger.ilpv4.connector.server.spring.settings.properties.ConnectorProperties.ADMIN_PASSWORD;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.interledger.crypto.CryptoConfigConstants.ILPV4_CONNECTOR_KEYSTORE_JKS_FILENAME;
import static org.interledger.crypto.CryptoConfigConstants.ILPV4_CONNECTOR_KEYSTORE_JKS_FILENAME_DEFAULT;
import static org.interledger.crypto.CryptoConfigConstants.ILPV4_CONNECTOR_KEYSTORE_JKS_PASSWORD;
import static org.interledger.crypto.CryptoConfigConstants.ILPV4_CONNECTOR_KEYSTORE_JKS_PASSWORD_DEFAULT;
import static org.interledger.crypto.CryptoConfigConstants.ILPV4_CONNECTOR_KEYSTORE_JKS_SECRET0_ALIAS;
import static org.interledger.crypto.CryptoConfigConstants.ILPV4_CONNECTOR_KEYSTORE_JKS_SECRET0_ALIAS_DEFAULT;
import static org.interledger.crypto.CryptoConfigConstants.ILPV4_CONNECTOR_KEYSTORE_JKS_SECRET0_PASSWORD;
import static org.interledger.crypto.CryptoConfigConstants.ILPV4_CONNECTOR_KEYSTORE_JKS_SECRET0_PASSWORD_DEFAULT;

/**
 * Ensures that the JavaMoney library is properly wired into the Connector.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  classes = {ConnectorServerConfig.class}
)
@ActiveProfiles({"test"})
@TestPropertySource(
  properties = {
    ADMIN_PASSWORD + "=password",
    ConnectorProperties.ENABLED_PROTOCOLS + "." + ConnectorProperties.BLAST_ENABLED + "=true",
    ILPV4_CONNECTOR_KEYSTORE_JKS_FILENAME + "=" + ILPV4_CONNECTOR_KEYSTORE_JKS_FILENAME_DEFAULT,
    ILPV4_CONNECTOR_KEYSTORE_JKS_PASSWORD + "=" + ILPV4_CONNECTOR_KEYSTORE_JKS_PASSWORD_DEFAULT,
    ILPV4_CONNECTOR_KEYSTORE_JKS_SECRET0_ALIAS + "=" + ILPV4_CONNECTOR_KEYSTORE_JKS_SECRET0_ALIAS_DEFAULT,
    ILPV4_CONNECTOR_KEYSTORE_JKS_SECRET0_PASSWORD + "=" + ILPV4_CONNECTOR_KEYSTORE_JKS_SECRET0_PASSWORD_DEFAULT
  }
)
public class JavaMoneyConfigTest {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  ExchangeRateProvider exchangeRateProvider;

  /**
   * Validate the "test connection" method in the IL-DCP requestor.
   */
  @Test
  public void testXrpExchangeRateProviderBeans() {
    final ExchangeRate actualExchangeRate = exchangeRateProvider.getExchangeRate(
      ConversionQueryBuilder.of()
        .setRateTypes(RateType.DEFERRED)
        .setBaseCurrency("XRP")
        .setTermCurrency("USD")
        .build()
    );

    logger.info("Loaded XRP-USD ExchangeRate: {}", actualExchangeRate);
    assertThat(actualExchangeRate.getCurrency().getCurrencyCode(), is("USD"));
    assertThat(actualExchangeRate.getBaseCurrency().getCurrencyCode(), is("XRP"));
    assertThat(actualExchangeRate.getFactor().compareTo(new DefaultNumberValue(BigInteger.ZERO)) > 0, is(true));
    assertThat(actualExchangeRate.getExchangeRateChain().size(), is(1));
  }

  /**
   * Validate the "test connection" method in the IL-DCP requestor.
   */
  @Test
  public void testIdentityExchangeRateProviderBeans() {
    final ExchangeRate actualExchangeRate = exchangeRateProvider.getExchangeRate(
      ConversionQueryBuilder.of()
        .setRateTypes(RateType.DEFERRED)
        .setBaseCurrency("XRP")
        .setTermCurrency("XRP")
        .build()
    );

    logger.info("Identity FX Rate: {}", actualExchangeRate);
    assertThat(actualExchangeRate.getCurrency().getCurrencyCode(), is("XRP"));
    assertThat(actualExchangeRate.getBaseCurrency().getCurrencyCode(), is("XRP"));
    assertThat(actualExchangeRate.getFactor().intValueExact(), is(1));
    assertThat(actualExchangeRate.getExchangeRateChain().size(), is(1));
  }

}