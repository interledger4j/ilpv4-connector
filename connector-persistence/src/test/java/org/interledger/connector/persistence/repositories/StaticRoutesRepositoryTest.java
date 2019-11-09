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
import org.assertj.core.api.Condition;
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
        .accountId(AccountId.of("mac"))
        .prefix(InterledgerAddressPrefix.of("g.philly.paddys"))
        .build();

    StaticRoute charlie = StaticRoute.builder()
        .accountId(AccountId.of("charlie"))
        .prefix(InterledgerAddressPrefix.of("g.philly.birdlaw"))
        .build();

    StaticRoute savedMac = saveAndGetRoute(mac);

    assertThat(staticRoutesRepository.getAllStaticRoutes())
        .hasSize(1)
        .extracting("id", "accountId", "prefix")
        .containsExactly(tuple(savedMac.id(), mac.accountId(), mac.prefix()));

    StaticRoute savedCharlie = saveAndGetRoute(charlie);

    assertThat(staticRoutesRepository.getAllStaticRoutes())
        .hasSize(2)
        .extracting("id", "accountId", "prefix")
        .containsExactly(
            tuple(savedMac.id(), mac.accountId(), mac.prefix()),
            tuple(savedCharlie.id(), charlie.accountId(), charlie.prefix())
        );

    staticRoutesRepository.deleteStaticRoute(mac.prefix());
    StaticRoute deletedRoute = staticRoutesRepository.getByPrefix(mac.prefix());
    assertThat(deletedRoute).isNull();

    assertThat(staticRoutesRepository.getAllStaticRoutes())
        .hasSize(1)
        .extracting("id", "accountId", "prefix")
        .containsExactly(tuple(savedCharlie.id(), charlie.accountId(), charlie.prefix()));
  }

  private StaticRoute saveAndGetRoute(StaticRoute routeToSave) {
    StaticRoute savedRoute = staticRoutesRepository.saveStaticRoute(routeToSave);

    assertThat(savedRoute).extracting("accountId", "prefix")
        .containsExactly(routeToSave.accountId(), routeToSave.prefix());

    StaticRoute fetchedRoute = staticRoutesRepository.getByPrefix(routeToSave.prefix());

    assertThat(fetchedRoute)
        .doesNotHave(nullId)
        .extracting("accountId", "prefix")
        .containsExactly(routeToSave.accountId(), routeToSave.prefix());
    return fetchedRoute;
  }

  private final Condition<StaticRoute> nullId = new Condition<StaticRoute>() {
    @Override
    public boolean matches(StaticRoute value) {
      return value.id() == null;
    }
  };

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
