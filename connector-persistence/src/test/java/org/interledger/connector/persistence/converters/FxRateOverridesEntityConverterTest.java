package org.interledger.connector.persistence.converters;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.fxrates.FxRateOverride;
import org.interledger.connector.persistence.entities.FxRateOverrideEntity;

import org.junit.Test;

import java.math.BigDecimal;

public class FxRateOverridesEntityConverterTest {

  @Test
  public void convert() {
    FxRateOverridesEntityConverter converter = new FxRateOverridesEntityConverter();

    FxRateOverride dto = FxRateOverride.builder()
        .id(1l)
        .rate(BigDecimal.ONE)
        .assetCodeTo("DAVEANDBUSTERS")
        .assetCodeFrom("CHUCKIECHEESE")
        .build();

    FxRateOverrideEntity entity = new FxRateOverrideEntity(dto);
    FxRateOverride convertedDto = converter.convert(entity);
    assertThat(convertedDto).extracting("id", "rate", "assetCodeTo", "assetCodeFrom")
        .containsExactly(dto.id(), dto.rate(), dto.assetCodeTo(), dto.assetCodeFrom());
    // test this separately from fields because we're trying to mirror the hibernate behavior
    assertThat(convertedDto).isEqualTo(dto);
    assertThat(new FxRateOverrideEntity(convertedDto)).isEqualTo(entity);
  }
}
