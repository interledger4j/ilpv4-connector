package org.interledger.connector.persistence.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.persistence.config.ConnectorPersistenceConfig;
import org.interledger.connector.persistence.converters.StaticRouteEntityConverter;
import org.interledger.connector.routing.StaticRoute;
import org.interledger.core.InterledgerAddressPrefix;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.Test;
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

import java.util.Set;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    ConnectorPersistenceConfig.class, StaticRoutesRepositoryTest.TestPersistenceConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
@AutoConfigureEmbeddedDatabase
public class StaticRoutesRepositoryTest {

  @Autowired
  private StaticRoutesRepository staticRoutesRepository;

  @Test
  public void saveAndLoadAndSaveSomeMore() {
    StaticRoute frankReynolds = StaticRoute.builder()
        .accountId(AccountId.of("frankReynolds"))
        .prefix(InterledgerAddressPrefix.of("g.philly.paddys"))
        .build();

    StaticRoute dennisReynolds = StaticRoute.builder()
        .accountId(AccountId.of("dennisReynolds"))
        .prefix(InterledgerAddressPrefix.of("g.philly.rangerover"))
        .build();

    Set<StaticRoute> routes = Sets.newHashSet(
        frankReynolds,
        dennisReynolds,
        StaticRoute.builder()
            .accountId(AccountId.of("ricketyCricket"))
            .prefix(InterledgerAddressPrefix.of("g.philly.shelter"))
            .build()
    );

    Set<StaticRoute> savedRoutes = staticRoutesRepository.saveAllStaticRoutes(routes);
    assertThat(savedRoutes).hasSize(3).extracting("accountId", "prefix")
        .containsOnly(
            tuple(AccountId.of("frankReynolds"), InterledgerAddressPrefix.of("g.philly.paddys")),
            tuple(AccountId.of("dennisReynolds"), InterledgerAddressPrefix.of("g.philly.rangerover")),
            tuple(AccountId.of("ricketyCricket"), InterledgerAddressPrefix.of("g.philly.shelter"))
        );

    assertThat(staticRoutesRepository.getAllStaticRoutes()).isEqualTo(savedRoutes);

    StaticRoute charlieKelley = StaticRoute.builder()
        .accountId(AccountId.of("charlieKelley"))
        .prefix(InterledgerAddressPrefix.of("g.philly.birdlaw"))
        .build();

    routes.remove(frankReynolds);
    routes.remove(dennisReynolds);
    routes.add(charlieKelley);
    frankReynolds = StaticRoute.builder()
        .accountId(AccountId.of("frankReynolds"))
        .prefix(InterledgerAddressPrefix.of("g.philly.prison"))
        .build();

    routes.add(frankReynolds);

    savedRoutes = staticRoutesRepository.saveAllStaticRoutes(routes);
    assertThat(savedRoutes).hasSize(3).extracting("accountId", "prefix")
        .containsOnly(
            tuple(AccountId.of("frankReynolds"), InterledgerAddressPrefix.of("g.philly.prison")),
            tuple(AccountId.of("ricketyCricket"), InterledgerAddressPrefix.of("g.philly.shelter")),
            tuple(AccountId.of("charlieKelley"), InterledgerAddressPrefix.of("g.philly.birdlaw"))
        );
  }

  @Test
  public void saveAndDelete() {
    StaticRoute mac = StaticRoute.builder()
        .accountId(AccountId.of("mac"))
        .prefix(InterledgerAddressPrefix.of("g.philly.paddys"))
        .build();

    StaticRoute savedRoute = staticRoutesRepository.saveStaticRoute(mac);

    assertThat(savedRoute).extracting("accountId", "prefix")
        .containsExactly(AccountId.of("mac"), InterledgerAddressPrefix.of("g.philly.paddys"));

    StaticRoute fetchedRoute = staticRoutesRepository.getByPrefix(InterledgerAddressPrefix.of("g.philly.paddys"));
    assertThat(fetchedRoute).extracting("accountId", "prefix")
        .containsExactly(AccountId.of("mac"), InterledgerAddressPrefix.of("g.philly.paddys"));

    staticRoutesRepository.deleteStaticRoute(InterledgerAddressPrefix.of("g.philly.paddys"));

    StaticRoute deletedRoute = staticRoutesRepository.getByPrefix(InterledgerAddressPrefix.of("g.philly.paddys"));
    assertThat(deletedRoute).isNull();
  }


  @Configuration("application.yml")
  public static class TestPersistenceConfig {

    ////////////////////////
    // SpringConverters
    ////////////////////////

    @Autowired
    private StaticRouteEntityConverter staticRouteEntityConverter;

    @Bean
    public ObjectMapper objectMapper() {
      return new ObjectMapper();
    }

    @Bean
    public ConfigurableConversionService conversionService() {
      ConfigurableConversionService conversionService = new DefaultConversionService();
      conversionService.addConverter(staticRouteEntityConverter);
      return conversionService;
    }
  }

}
