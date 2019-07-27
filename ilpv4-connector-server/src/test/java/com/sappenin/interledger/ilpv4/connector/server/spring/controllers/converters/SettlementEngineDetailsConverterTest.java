package com.sappenin.interledger.ilpv4.connector.server.spring.controllers.converters;

import okhttp3.HttpUrl;
import org.interledger.connector.accounts.SettlementEngineDetails;
import org.interledger.ilpv4.connector.persistence.entities.SettlementEngineDetailsEntity;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit tests for {@link SettlementEngineDetailsConverter}.
 */
public class SettlementEngineDetailsConverterTest {

  private SettlementEngineDetailsConverter converter;

  @Before
  public void setup() {
    this.converter = new SettlementEngineDetailsConverter();
  }

  @Test
  public void convert() {
    final SettlementEngineDetails settlementEngineDetails = SettlementEngineDetails.builder()
      .baseUrl(HttpUrl.parse("https://example.com"))
      .assetScale(2)
      .settlementEngineAccountId(UUID.randomUUID().toString())
      .build();
    final SettlementEngineDetailsEntity entity = new SettlementEngineDetailsEntity(settlementEngineDetails);

    SettlementEngineDetails actual = converter.convert(entity);

    assertThat(actual.baseUrl(), is(settlementEngineDetails.baseUrl()));
    assertThat(actual.settlementEngineAccountId(), is(settlementEngineDetails.settlementEngineAccountId()));
    assertThat(actual.assetScale(), is(settlementEngineDetails.assetScale()));
  }
}
