package org.interledger.connector.server.javamoney;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.interledger.connector.fx.JavaMoneyUtils;
import org.interledger.connector.server.ConnectorServerConfig;

import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.primitives.UnsignedLong;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.MonetaryAmount;
import javax.money.convert.ConversionQuery;
import javax.money.convert.CurrencyConversion;
import javax.money.convert.CurrencyConversionException;
import javax.money.convert.ExchangeRate;
import javax.money.convert.ExchangeRateProvider;
import javax.money.convert.MonetaryConversions;

/**
 * Ensures that we don't need to fix this issue: https://github.com/interledger4j/ilpv4-connector/issues/307
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {ConnectorServerConfig.class}
)
@ActiveProfiles( {"test"})
public class JavaMoneyProviderTest {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  ExchangeRateProvider exchangeRateProvider;

  @Autowired
  private JavaMoneyUtils javaMoneyUtils;

  @Autowired
  private Cache<ConversionQuery, ExchangeRate> fxCache;
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @MockBean
  @Qualifier("fx")
  private RestTemplate restTemplate;

  @Test
  public void testProviderFails() {
    final CurrencyUnit sourceCurrencyUnit = Monetary.getCurrency("XRP");
    final int sourceScale = 0;
    final MonetaryAmount sourceAmount =
      javaMoneyUtils.toMonetaryAmount(sourceCurrencyUnit, BigInteger.valueOf(1000), sourceScale);

    final CurrencyUnit destinationCurrencyUnit = Monetary.getCurrency("USD");
    final int destinationScale = 0;

    ResponseEntity<Map<String, String>> badResponse = new ResponseEntity<>(new HashMap<>(), HttpStatus.SERVICE_UNAVAILABLE);
    when(restTemplate.exchange(any(), any(), any(), any(ParameterizedTypeReference.class), any(), any()))
      .thenReturn(badResponse);

    fxCache.invalidateAll();
    Function<CurrencyUnit, CurrencyConversion> currencyConverter = (CurrencyUnit unit) -> MonetaryConversions.getConversion(unit);

    expectedException.expect(CurrencyConversionException.class);
    CurrencyConversion destCurrencyConversion = currencyConverter.apply(destinationCurrencyUnit);

    // Since CurrencyConversion is lazily initialized, we have to actually use it to get the exception
    UnsignedLong.valueOf(
      javaMoneyUtils.toInterledgerAmount(sourceAmount.with(destCurrencyConversion), destinationScale));
  }
}
