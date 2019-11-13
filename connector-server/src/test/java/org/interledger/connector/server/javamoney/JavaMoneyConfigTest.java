package org.interledger.connector.server.javamoney;

import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.connector.core.ConfigConstants.ADMIN_PASSWORD;
import static org.interledger.crypto.CryptoConfigConstants.INTERLEDGER_CONNECTOR_KEYSTORE_JKS_FILENAME;
import static org.interledger.crypto.CryptoConfigConstants.INTERLEDGER_CONNECTOR_KEYSTORE_JKS_FILENAME_DEFAULT;
import static org.interledger.crypto.CryptoConfigConstants.INTERLEDGER_CONNECTOR_KEYSTORE_JKS_PASSWORD;
import static org.interledger.crypto.CryptoConfigConstants.INTERLEDGER_CONNECTOR_KEYSTORE_JKS_PASSWORD_DEFAULT;
import static org.interledger.crypto.CryptoConfigConstants.INTERLEDGER_CONNECTOR_KEYSTORE_JKS_SECRET0_ALIAS;
import static org.interledger.crypto.CryptoConfigConstants.INTERLEDGER_CONNECTOR_KEYSTORE_JKS_SECRET0_ALIAS_DEFAULT;
import static org.interledger.crypto.CryptoConfigConstants.INTERLEDGER_CONNECTOR_KEYSTORE_JKS_SECRET0_PASSWORD;
import static org.interledger.crypto.CryptoConfigConstants.INTERLEDGER_CONNECTOR_KEYSTORE_JKS_SECRET0_PASSWORD_DEFAULT;

import org.interledger.connector.core.ConfigConstants;
import org.interledger.connector.server.ConnectorServerConfig;

import okhttp3.OkHttpClient;
import org.javamoney.moneta.spi.DefaultNumberValue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigInteger;
import javax.money.convert.ConversionQueryBuilder;
import javax.money.convert.ExchangeRate;
import javax.money.convert.ExchangeRateProvider;
import javax.money.convert.RateType;

/**
 * Ensures that the JavaMoney library is properly wired into the Connector.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {ConnectorServerConfig.class}
)
@ActiveProfiles( {"test"})
@TestPropertySource(
    properties = {
        ADMIN_PASSWORD + "=password",
        ConfigConstants.ENABLED_PROTOCOLS + "." + ConfigConstants.ILP_OVER_HTTP_ENABLED + "=true",
        INTERLEDGER_CONNECTOR_KEYSTORE_JKS_FILENAME + "=" + INTERLEDGER_CONNECTOR_KEYSTORE_JKS_FILENAME_DEFAULT,
        INTERLEDGER_CONNECTOR_KEYSTORE_JKS_PASSWORD + "=" + INTERLEDGER_CONNECTOR_KEYSTORE_JKS_PASSWORD_DEFAULT,
        INTERLEDGER_CONNECTOR_KEYSTORE_JKS_SECRET0_ALIAS + "=" + INTERLEDGER_CONNECTOR_KEYSTORE_JKS_SECRET0_ALIAS_DEFAULT,
        INTERLEDGER_CONNECTOR_KEYSTORE_JKS_SECRET0_PASSWORD + "=" + INTERLEDGER_CONNECTOR_KEYSTORE_JKS_SECRET0_PASSWORD_DEFAULT
    }
)
public class JavaMoneyConfigTest {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  ExchangeRateProvider exchangeRateProvider;

  @Autowired
  @Qualifier("fx")
  private OkHttpClient fxHttpClient;

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
    assertThat(actualExchangeRate.getCurrency().getCurrencyCode()).isEqualTo(("USD"));
    assertThat(actualExchangeRate.getBaseCurrency().getCurrencyCode()).isEqualTo(("XRP"));
    assertThat(actualExchangeRate.getFactor().compareTo(new DefaultNumberValue(BigInteger.ZERO)) > 0).isEqualTo((true));
    assertThat(actualExchangeRate.getExchangeRateChain().size()).isEqualTo((1));
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
    assertThat(actualExchangeRate.getCurrency().getCurrencyCode()).isEqualTo(("XRP"));
    assertThat(actualExchangeRate.getBaseCurrency().getCurrencyCode()).isEqualTo(("XRP"));
    assertThat(actualExchangeRate.getFactor().intValueExact()).isEqualTo((1));
    assertThat(actualExchangeRate.getExchangeRateChain().size()).isEqualTo((1));
  }

  @Test
  public void okhttpConfigFileOverrides() {
    assertThat(fxHttpClient.readTimeoutMillis()).isEqualTo(30000);
    assertThat(fxHttpClient.writeTimeoutMillis()).isEqualTo(40000);
    assertThat(fxHttpClient.connectTimeoutMillis()).isEqualTo(5000);
  }

}
