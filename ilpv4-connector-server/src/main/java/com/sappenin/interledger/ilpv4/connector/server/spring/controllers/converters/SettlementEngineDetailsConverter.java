package com.sappenin.interledger.ilpv4.connector.server.spring.controllers.converters;

import okhttp3.HttpUrl;
import org.interledger.connector.accounts.SettlementEngineDetails;
import org.interledger.ilpv4.connector.persistence.entities.SettlementEngineDetailsEntity;
import org.springframework.core.convert.converter.Converter;

import java.util.Objects;

/**
 * A converter from {@link SettlementEngineDetailsEntity} to {@link SettlementEngineDetails}.
 */
public class SettlementEngineDetailsConverter implements
  Converter<SettlementEngineDetailsEntity, SettlementEngineDetails> {

  @Override
  public SettlementEngineDetails convert(final SettlementEngineDetailsEntity settlementEngineDetailsEntity) {
    Objects.requireNonNull(settlementEngineDetailsEntity);
    return SettlementEngineDetails.builder()
      .settlementEngineAccountId(settlementEngineDetailsEntity.getAccountId())
      .baseUrl(HttpUrl.parse(settlementEngineDetailsEntity.getBaseUrl()))
      .assetScale(settlementEngineDetailsEntity.getAssetScale())
      .build();
  }
}
