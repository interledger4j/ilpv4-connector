package org.interledger.connector.javax.money.providers;

import com.google.common.collect.Maps;
import org.interledger.connector.javax.money.providers.CryptoCompareRateProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import javax.money.convert.ConversionQueryBuilder;
import javax.money.convert.ExchangeRate;
import javax.money.convert.RateType;
import java.math.BigDecimal;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CryptoCompareRateProvider}.
 */
public class CryptoCompareRateProviderTest {

  @Mock
  RestTemplate restTemplate;
  @Mock
  ResponseEntity<Map<String, String>> responseEntityMock;

  private Map<String, String> ratesResponseMap;

  private CryptoCompareRateProvider provider;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    this.provider = new CryptoCompareRateProvider(() -> "apiKey", restTemplate);
    this.ratesResponseMap = Maps.newHashMap();

    final ResponseEntity<Map<String, String>> responseEntityMock = mock(ResponseEntity.class);
    when(restTemplate.exchange(any(), any(), any(),
      Mockito.<ParameterizedTypeReference<Map<String, String>>>any(), anyString(), anyString())
    ).thenReturn(responseEntityMock);

    when(responseEntityMock.getBody()).thenReturn(ratesResponseMap);
  }

  @Test(expected = RuntimeException.class)
  public void getExchangeRateWithUnknownCurrency() {
    try {
      provider.getExchangeRate(
        ConversionQueryBuilder.of().setBaseCurrency("FOO").setTermCurrency("USD").setRateTypes(RateType.DEFERRED)
          .build()
      );
    } catch (RuntimeException e) {
      assertThat(e.getMessage(), is("Unknown currency code: FOO"));
      throw e;
    }
  }

  @Test(expected = RuntimeException.class)
  public void getExchangeRateWithNoRateFound() {
    when(responseEntityMock.getBody()).thenReturn(Maps.newHashMap());
    try {
      provider.getExchangeRate(
        ConversionQueryBuilder.of().setBaseCurrency("XRP").setTermCurrency("USD").setRateTypes(RateType.DEFERRED)
          .build()
      );
    } catch (RuntimeException e) {
      assertThat(e.getMessage(), is("Failed to load currency conversion data"));
      throw e;
    }
  }

  @Test
  public void getExchangeRate() {
    ratesResponseMap.put("USD", "0.31234");
    final ExchangeRate actual = provider.getExchangeRate(
      ConversionQueryBuilder.of().setBaseCurrency("XRP").setTermCurrency("USD").setRateTypes(RateType.DEFERRED).build()
    );

    assertThat(actual.getBaseCurrency().getCurrencyCode(), is("XRP"));
    assertThat(actual.getCurrency().getCurrencyCode(), is("USD"));
    assertThat(actual.getExchangeRateChain().size(), is(1));
    assertThat(actual.getFactor().numberValue(BigDecimal.class).compareTo(BigDecimal.ZERO) > 0, is(true));
    assertThat(actual.getContext().getProviderName(), is("CC"));
    assertThat(actual.getContext().getRateType(), is(RateType.DEFERRED));
  }
}
