package org.interledger.connector.persistence.converters;

import org.interledger.connector.fxrates.FxRateOverride;
import org.interledger.connector.persistence.entities.FxRateOverrideEntity;

import org.springframework.core.convert.converter.Converter;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public class FxRateOverridesEntityConverter implements Converter<FxRateOverrideEntity, FxRateOverride> {

  @Override
  public FxRateOverride convert(FxRateOverrideEntity rateOverrideEntity) {
    Objects.requireNonNull(rateOverrideEntity);

    return FxRateOverride.builder()
        .id(rateOverrideEntity.getId())
        .assetCodeFrom(rateOverrideEntity.getAssetCodeFrom())
        .assetCodeTo(rateOverrideEntity.getAssetCodeTo())
        .rate(rateOverrideEntity.getRate())
        .createdAt(Optional.ofNullable(rateOverrideEntity.getCreatedDate()).orElseGet(() -> Instant.now()))
        .modifiedAt(Optional.ofNullable(rateOverrideEntity.getModifiedDate()).orElseGet(() -> Instant.now()))
        .build();
  }
}
