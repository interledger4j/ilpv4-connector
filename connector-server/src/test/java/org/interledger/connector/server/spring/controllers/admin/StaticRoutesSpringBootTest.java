package org.interledger.connector.server.spring.controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.connector.server.spring.controllers.PathConstants.SLASH_ROUTES;
import static org.interledger.connector.server.spring.controllers.PathConstants.SLASH_ROUTES_STATIC;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.routing.StaticRoute;
import org.interledger.connector.server.ConnectorServerConfig;
import org.interledger.core.InterledgerAddressPrefix;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
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

import java.util.Map;
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
    StaticRoute charlie = charlieKelleyRoute();

    assertPutRoute(charlie, HttpStatus.CREATED);
    assertThat(getStaticRoutes()).hasSize(1).containsOnly(charlie);

    StaticRoute frank = frankReynoldsRoute();

    assertPutRoute(frank, HttpStatus.CREATED);
    assertThat(getStaticRoutes()).hasSize(2).containsOnly(charlie, frank);

    Set<StaticRoute> allRoutes = getRoutes();
    assertThat(allRoutes).hasSize(2).containsOnly(charlie, frank);

    deleteRoute(charlie);
    assertThat(getStaticRoutes()).hasSize(1).containsOnly(frank);

    deleteRoute(frank);
    assertThat(getStaticRoutes()).isEmpty();
  }

  /**
   * Verify API ignores unknown properties
   */
  @Test
  public void testJsonMarshalling() {

    InterledgerAddressPrefix prefix = InterledgerAddressPrefix.of("g.foo.baz");
    Map<String, Object> rawValues = ImmutableMap.<String, Object>builder()
        .put("accountId", AccountId.of("testJsonMarshalling"))
        .put("prefix", prefix)
        .put("whatIsThatSmell", "ifYouHaveToAskYouDoNotWantToKnow")
        .build();

    final HttpEntity httpEntity = new HttpEntity(rawValues, authHeaders());

    ResponseEntity response =
        restTemplate.exchange(SLASH_ROUTES_STATIC + "/" + prefix.getValue(), HttpMethod.PUT, httpEntity, Void.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
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

  private StaticRoute ricketyCricketRoute() {
    return StaticRoute.builder()
        .accountId(AccountId.of("ricketyCricket"))
        .prefix(InterledgerAddressPrefix.of("g.philly.shelter"))
        .build();
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

  private Set<StaticRoute> getStaticRoutes() {
    final HttpEntity requestBody = new HttpEntity(authHeaders());

    ResponseEntity<Set<StaticRoute>> routes = restTemplate.exchange(SLASH_ROUTES_STATIC, HttpMethod.GET, requestBody,
        new ParameterizedTypeReference<Set<StaticRoute>>() {});
    return routes.getBody();
  }

}
