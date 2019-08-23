package org.interledger.connector.persistence.converters;

import okhttp3.HttpUrl;
import org.interledger.connector.accounts.SettlementEngineAccountId;
import org.interledger.connector.accounts.SettlementEngineDetails;
import org.interledger.connector.persistence.entities.SettlementEngineDetailsEntity;
import org.springframework.core.convert.converter.Converter;

import java.util.Objects;

/**
 * A converter from {@link SettlementEngineDetailsEntity} to {@link SettlementEngineDetails}.
 */
public class SettlementEngineDetailsEntityConverter implements
  Converter<SettlementEngineDetailsEntity, SettlementEngineDetails> {

  @Override
  public SettlementEngineDetails convert(final SettlementEngineDetailsEntity settlementEngineDetailsEntity) {
    Objects.requireNonNull(settlementEngineDetailsEntity);
    return SettlementEngineDetails.builder()
      .settlementEngineAccountId(
        SettlementEngineAccountId.of(settlementEngineDetailsEntity.getSettlementEngineAccountId())
      )
      .baseUrl(HttpUrl.parse(settlementEngineDetailsEntity.getBaseUrl()))
      .customSettings(settlementEngineDetailsEntity.getCustomSettings())
      .build();
  }
}
