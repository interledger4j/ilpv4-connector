package org.interledger.connector.persistence.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.persistence.config.ConnectorPersistenceConfig;
import org.interledger.connector.persistence.converters.StaticRouteEntityConverter;
import org.interledger.connector.routing.StaticRoute;
import org.interledger.core.InterledgerAddressPrefix;

import com.fasterxml.jackson.databind.ObjectMapper;
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
  public void saveGetSaveGetDeleteGet() {
    StaticRoute mac = StaticRoute.builder()
        .nextHopAccountId(AccountId.of("mac"))
        .routePrefix(InterledgerAddressPrefix.of("g.philly.paddys"))
        .build();

    StaticRoute charlie = StaticRoute.builder()
        .nextHopAccountId(AccountId.of("charlie"))
        .routePrefix(InterledgerAddressPrefix.of("g.philly.birdlaw"))
        .build();

    saveAndGetRoute(mac);

    assertThat(staticRoutesRepository.getAllStaticRoutes())
        .hasSize(1)
        .extracting("nextHopAccountId", "routePrefix")
        .containsExactly(tuple(mac.nextHopAccountId(), mac.routePrefix()));

    saveAndGetRoute(charlie);

    assertThat(staticRoutesRepository.getAllStaticRoutes())
        .hasSize(2)
        .extracting("nextHopAccountId", "routePrefix")
        .containsExactly(
            tuple(mac.nextHopAccountId(), mac.routePrefix()),
            tuple(charlie.nextHopAccountId(), charlie.routePrefix())
        );

    // delete only really deletes the first time
    assertThat(staticRoutesRepository.deleteStaticRouteByPrefix(mac.routePrefix())).isTrue();
    assertThat(staticRoutesRepository.deleteStaticRouteByPrefix(mac.routePrefix())).isFalse();

    StaticRoute deletedRoute = staticRoutesRepository.findByAddressPrefix(mac.routePrefix());
    assertThat(deletedRoute).isNull();

    assertThat(staticRoutesRepository.getAllStaticRoutes())
        .hasSize(1)
        .extracting("nextHopAccountId", "routePrefix")
        .containsExactly(tuple(charlie.nextHopAccountId(), charlie.routePrefix()));
  }

  private StaticRoute saveAndGetRoute(StaticRoute routeToSave) {
    StaticRoute savedRoute = staticRoutesRepository.saveStaticRoute(routeToSave);

    assertThat(savedRoute).extracting("nextHopAccountId", "routePrefix")
        .containsExactly(routeToSave.nextHopAccountId(), routeToSave.routePrefix());

    StaticRoute fetchedRoute = staticRoutesRepository.findByAddressPrefix(routeToSave.routePrefix());

    assertThat(fetchedRoute)
        .extracting("nextHopAccountId", "routePrefix")
        .containsExactly(routeToSave.nextHopAccountId(), routeToSave.routePrefix());
    return fetchedRoute;
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
