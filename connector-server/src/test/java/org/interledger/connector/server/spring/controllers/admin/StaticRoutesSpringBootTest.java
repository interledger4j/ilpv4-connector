package org.interledger.connector.server.spring.controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.connector.server.spring.controllers.PathConstants.SLASH_ROUTES;
import static org.interledger.connector.server.spring.controllers.PathConstants.SLASH_ROUTES_STATIC;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.routing.StaticRoute;
import org.interledger.connector.server.ConnectorServerConfig;
import org.interledger.core.InterledgerAddressPrefix;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Set;

/**
 * Running-server test that validates behavior of StaticRoutesController
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {ConnectorServerConfig.class}
)
@ActiveProfiles( {"test"})
public class StaticRoutesSpringBootTest {

  private static final String PASSWORD = "password";
  private static final String ADMIN = "admin";

  @Autowired
  TestRestTemplate restTemplate;

  @Autowired
  private ObjectMapper objectMapper;

  private BasicJsonTester jsonTester = new BasicJsonTester(getClass());

  @Test
  public void createIndividualAndDelete() {
    StaticRoute staticRoute = charlieKelleyRoute();

    assertPutRoute(staticRoute, HttpStatus.CREATED);
    assertThat(getRoutes()).hasSize(1).containsOnly(charlieKelleyRoute());

    deleteRoute(staticRoute);
    assertThat(getRoutes()).isEmpty();
  }

  private StaticRoute ricketyCricketRoute() {
    return StaticRoute.builder()
        .accountId(AccountId.of("ricketyCricket"))
        .prefix(InterledgerAddressPrefix.of("g.philly.shelter"))
        .build();
  }

  private StaticRoute frankReynoldsRoute() {
    return StaticRoute.builder()
        .accountId(AccountId.of("frankReynolds"))
        .prefix(InterledgerAddressPrefix.of("g.philly.paddys"))
        .build();
  }

  private StaticRoute charlieKelleyRoute() {
    return StaticRoute.builder()
        .accountId(AccountId.of("charlieKelley"))
        .prefix(InterledgerAddressPrefix.of("g.philly.birdlaw"))
        .build();
  }

  private void assertPutRoutes(Set<StaticRoute> routes, HttpStatus expectedStatus) {
    final HttpHeaders headers = authHeaders();
    final HttpEntity httpEntity = new HttpEntity(routes, headers);

    ResponseEntity<Set<StaticRoute>> savedRoutes = restTemplate.exchange(SLASH_ROUTES_STATIC, HttpMethod.PUT,
        httpEntity, new ParameterizedTypeReference<Set<StaticRoute>>() {});

    assertThat(savedRoutes.getStatusCode()).isEqualTo(expectedStatus);
    assertThat(savedRoutes.getBody()).hasSize(2)
        .isEqualTo(routes)
        .extracting("id").doesNotContainNull();
  }

  private HttpHeaders authHeaders() {
    final HttpHeaders headers = new HttpHeaders();
    headers.setBasicAuth(ADMIN, PASSWORD);
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

  private void assertPutRoute(StaticRoute route, HttpStatus expectedStatus) {
    final HttpEntity httpEntity = new HttpEntity(route, authHeaders());

    ResponseEntity<StaticRoute> savedRoutes = restTemplate
        .exchange(SLASH_ROUTES_STATIC + "/" + route.prefix().getValue(), HttpMethod.PUT, httpEntity,
            StaticRoute.class);

    assertThat(savedRoutes.getStatusCode()).isEqualTo(expectedStatus);
    assertThat(savedRoutes.getBody())
        .isEqualTo(route)
        .extracting("id").isNotNull();
  }

  private void deleteRoute(StaticRoute route) {
    final HttpEntity httpEntity = new HttpEntity(authHeaders());
    String prefix = route.prefix().getValue();
    restTemplate.exchange(SLASH_ROUTES_STATIC + "/" + prefix, HttpMethod.DELETE, httpEntity,
        Void.class);
  }

  private Set<StaticRoute> getRoutes() {
    final HttpEntity requestBody = new HttpEntity(authHeaders());

    ResponseEntity<Set<StaticRoute>> routes = restTemplate.exchange(SLASH_ROUTES, HttpMethod.GET, requestBody,
        new ParameterizedTypeReference<Set<StaticRoute>>() {});
    return routes.getBody();
  }

}
