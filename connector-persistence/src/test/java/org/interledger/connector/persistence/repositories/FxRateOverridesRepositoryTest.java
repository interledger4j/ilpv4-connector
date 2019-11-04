package org.interledger.connector.persistence.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.interledger.connector.fxrates.FxRateOverride;
import org.interledger.connector.persistence.config.ConnectorPersistenceConfig;
import org.interledger.connector.persistence.converters.FxRateOverridesEntityConverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.math.BigDecimal;
import java.util.Set;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    ConnectorPersistenceConfig.class, FxRateOverridesRepositoryTest.TestPersistenceConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
@AutoConfigureEmbeddedDatabase
public class FxRateOverridesRepositoryTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Autowired
  private FxRateOverridesRepository repository;

  @Test
  public void saveAndLoadAndSaveSomeMore() {
    FxRateOverride originalEurToUsd = FxRateOverride.builder()
        .assetCodeFrom("EUR")
        .assetCodeTo("USD")
        .rate(BigDecimal.valueOf(1.12))
        .build();

    Set<FxRateOverride> overrides = Sets.newHashSet(
        originalEurToUsd,
        FxRateOverride.builder()
            .assetCodeFrom("USD")
            .assetCodeTo("JPY")
            .rate(BigDecimal.valueOf(105))
            .build(),
        FxRateOverride.builder()
            .assetCodeFrom("USD")
            .assetCodeTo("CAD")
            .rate(BigDecimal.valueOf(1.23))
            .build()
    );

    Set<FxRateOverride> savedOverrides = repository.saveAllOverrides(overrides);
    assertThat(savedOverrides).hasSize(3).extracting("assetCodeFrom", "assetCodeTo", "rate")
        .containsOnly(
            tuple("EUR", "USD", BigDecimal.valueOf(1.12)),
            tuple("USD", "JPY", BigDecimal.valueOf(105)),
            tuple("USD", "CAD", BigDecimal.valueOf(1.23))
        );

    assertThat(repository.getAllOverrides()).isEqualTo(savedOverrides);

    FxRateOverride eurToUsd = savedOverrides.stream().filter(fx -> fx.assetCodeFrom().equals("EUR")).findAny().get();

    overrides.remove(originalEurToUsd);
    overrides.add(
        FxRateOverride.builder()
            .id(eurToUsd.id())
            .assetCodeFrom("EUR")
            .assetCodeTo("USD")
            .rate(BigDecimal.valueOf(1.09))
            .build()
    );

    overrides.add(
        FxRateOverride.builder()
            .assetCodeFrom("EUR")
            .assetCodeTo("AUD")
            .rate(BigDecimal.valueOf(1.50))
            .build()
    );

    savedOverrides = repository.saveAllOverrides(overrides);
    assertThat(savedOverrides).hasSize(4).extracting("assetCodeFrom", "assetCodeTo", "rate")
        .containsOnly(
            tuple("EUR", "USD", BigDecimal.valueOf(1.09)),
            tuple("USD", "JPY", BigDecimal.valueOf(105)),
            tuple("USD", "CAD", BigDecimal.valueOf(1.23)),
            tuple("EUR", "AUD", BigDecimal.valueOf(1.50))
        );
  }

  @Configuration("application.yml")
  public static class TestPersistenceConfig {

    ////////////////////////
    // SpringConverters
    ////////////////////////

    @Autowired
    private FxRateOverridesEntityConverter fxRateOverrideEntityConverter;

    @Bean
    ObjectMapper objectMapper() {
      return new ObjectMapper();
    }

    @Bean
    ConfigurableConversionService conversionService() {
      ConfigurableConversionService conversionService = new DefaultConversionService();
      conversionService.addConverter(fxRateOverrideEntityConverter);
      return conversionService;
    }
  }
}
