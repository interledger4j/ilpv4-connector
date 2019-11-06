package org.interledger.connector.persistence.converters;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.persistence.entities.StaticRouteEntity;
import org.interledger.connector.routing.StaticRoute;
import org.interledger.core.InterledgerAddressPrefix;

import org.junit.Test;

public class StaticRouteEntityConverterTest {

  @Test
  public void convert() {
    StaticRouteEntityConverter converter = new StaticRouteEntityConverter();

    StaticRoute route = StaticRoute.builder()
        .id(1l)
        .prefix(InterledgerAddressPrefix.of("g.example"))
        .accountId(AccountId.of("foo"))
        .build();

    StaticRouteEntity entity = new StaticRouteEntity(route);
    StaticRoute converted = converter.convert(entity);

    assertThat(converted).extracting("id", "prefix", "accountId")
        .containsExactly(route.id(), route.prefix(), route.accountId());
    // test this separately from fields because we're trying to mirror the hibernate behavior
    assertThat(converted).isEqualTo(route);
    assertThat(entity).isEqualTo(new StaticRouteEntity(converted));
  }
}
