package org.interledger.connector.server.spring.controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.interledger.connector.server.spring.controllers.PathConstants.SLASH_ROUTES;
import static org.interledger.connector.server.spring.controllers.PathConstants.SLASH_ROUTES_STATIC;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.routing.Route;
import org.interledger.connector.routing.StaticRoute;
import org.interledger.connector.server.ConnectorServerConfig;
import org.interledger.core.InterledgerAddressPrefix;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.boot.test.json.JsonContentAssert;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
  private TestRestTemplate restTemplate;

  @Autowired
  private ObjectMapper objectMapper;

  private BasicJsonTester jsonTester = new BasicJsonTester(getClass());

  @Before
  public void setUp() throws Exception {
    // "empty" what's there before each test
    getRoutes().forEach(r -> this.deleteRoute(r.routePrefix()));
  }

  @Test
  public void createIndividualAndDelete() throws Exception {
    StaticRoute charlie = charlieKelleyRoute();

    assertPutRoute(charlie, HttpStatus.CREATED);
    assertThat(getStaticRoutes()).hasSize(1).containsOnly(charlie);

    StaticRoute frank = frankReynoldsRoute();

    assertPutRoute(frank, HttpStatus.CREATED);
    assertThat(getStaticRoutes()).hasSize(2).containsOnly(charlie, frank);

    Collection<Route> allRoutes = getRoutes();
    assertThat(allRoutes).hasSize(2)
        .extracting("nextHopAccountId", "routePrefix")
        .containsOnly(
            tuple(charlie.nextHopAccountId(), charlie.routePrefix()),
            tuple(frank.nextHopAccountId(), frank.routePrefix())
        );

    assertThat(deleteRoute(charlie.routePrefix()).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(deleteRoute(charlie.routePrefix()).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(getStaticRoutes()).hasSize(1).containsOnly(frank);

    assertThat(deleteRoute(frank.routePrefix()).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(deleteRoute(frank.routePrefix()).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(getStaticRoutes()).isEmpty();
  }

  @Test
  public void deleteNotFoundErrorMessage() {
    StaticRoute mcpoyle = StaticRoute.builder()
        .routePrefix(InterledgerAddressPrefix.of("g.philly.dairy"))
        .nextHopAccountId(AccountId.of("mcpoyle"))
        .build();
    ResponseEntity<String> response = deleteRoute(mcpoyle.routePrefix());

    JsonContentAssert assertJson = assertThat(jsonTester.from(response.getBody()));
    assertJson.extractingJsonPathValue("status").isEqualTo(404);
    assertJson.extractingJsonPathValue("title").isEqualTo("Static Route Does Not Exist (`g.philly.dairy`)");
    assertJson.extractingJsonPathValue("prefix").isEqualTo(mcpoyle.routePrefix().getValue());
    assertJson.extractingJsonPathValue("type")
        .isEqualTo("https://errors.interledger.org/routes/static/static-route-not-found");
  }

  @Test
  public void cannotCreateWhatAlreadyExists() {
    StaticRoute ricketyCricket = ricketyCricketRoute();
    assertPutRoute(ricketyCricket, HttpStatus.CREATED);

    final HttpEntity httpEntity = new HttpEntity(ricketyCricket, authHeaders());
    ResponseEntity<String> response = restTemplate
        .exchange(SLASH_ROUTES_STATIC + "/" + ricketyCricket.routePrefix().getValue(), HttpMethod.PUT, httpEntity,
            String.class);

    JsonContentAssert assertJson = assertThat(jsonTester.from(response.getBody()));
    assertJson.extractingJsonPathValue("status").isEqualTo(409);
    assertJson.extractingJsonPathValue("title").isEqualTo("Static Route Already Exists (`g.philly.shelter`)");
    assertJson.extractingJsonPathValue("prefix").isEqualTo(ricketyCricket.routePrefix().getValue());
    assertJson.extractingJsonPathValue("type")
        .isEqualTo("https://errors.interledger.org/routes/static/static-route-already-exists");
  }

  @Test
  public void mismatchedPrefixIsUnprocessable() {
    StaticRoute ricketyCricket = ricketyCricketRoute();

    StaticRoute ricketyMismatch = StaticRoute.builder()
        .nextHopAccountId(ricketyCricket.nextHopAccountId())
        .routePrefix(InterledgerAddressPrefix.of("g.philly.priesthood"))
        .build();

    final HttpEntity httpEntity = new HttpEntity(ricketyMismatch, authHeaders());

    ResponseEntity<String> response = restTemplate
        .exchange(SLASH_ROUTES_STATIC + "/" + ricketyCricket.routePrefix().getValue(), HttpMethod.PUT, httpEntity,
            String.class);

    JsonContentAssert assertJson = assertThat(jsonTester.from(response.getBody()));
    assertJson.extractingJsonPathValue("status").isEqualTo(422);
    assertJson.extractingJsonPathValue("title")
        .isEqualTo("Static Route Unprocessable [entityPrefix: `g.philly.priesthood`, urlPrefix: `g.philly.shelter`]");
    assertJson.extractingJsonPathValue("prefix").isEqualTo(ricketyMismatch.routePrefix().getValue());
    assertJson.extractingJsonPathValue("type")
        .isEqualTo("https://errors.interledger.org/routes/static/static-route-unprocessable");
  }

  /**
   * Verify API ignores unknown properties
   */
  @Test
  public void testJsonMarshalling() {

    InterledgerAddressPrefix prefix = InterledgerAddressPrefix.of("g.foo.baz");
    Map<String, Object> rawValues = ImmutableMap.<String, Object>builder()
        .put("nextHopAccountId", AccountId.of("testJsonMarshalling"))
        .put("routePrefix", prefix)
        .put("whatIsThatSmell", "ifYouHaveToAskYouDoNotWantToKnow")
        .build();

    final HttpEntity httpEntity = new HttpEntity(rawValues, authHeaders());

    ResponseEntity response =
        restTemplate.exchange(SLASH_ROUTES_STATIC + "/" + prefix.getValue(), HttpMethod.PUT, httpEntity, Void.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
  }

  private StaticRoute frankReynoldsRoute() {
    return StaticRoute.builder()
        .nextHopAccountId(AccountId.of("frankReynolds"))
        .routePrefix(InterledgerAddressPrefix.of("g.philly.paddys"))
        .build();
  }

  private StaticRoute charlieKelleyRoute() {
    return StaticRoute.builder()
        .nextHopAccountId(AccountId.of("charlieKelley"))
        .routePrefix(InterledgerAddressPrefix.of("g.philly.birdlaw"))
        .build();
  }

  private StaticRoute ricketyCricketRoute() {
    return StaticRoute.builder()
        .nextHopAccountId(AccountId.of("ricketyCricket"))
        .routePrefix(InterledgerAddressPrefix.of("g.philly.shelter"))
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
        .exchange(SLASH_ROUTES_STATIC + "/" + route.routePrefix().getValue(), HttpMethod.PUT, httpEntity,
            StaticRoute.class);

    assertThat(savedRoutes.getStatusCode()).isEqualTo(expectedStatus);
    assertThat(savedRoutes.getBody())
        .isEqualTo(route);
  }

  private ResponseEntity<String> deleteRoute(InterledgerAddressPrefix routePrefix) {
    final HttpEntity httpEntity = new HttpEntity(authHeaders());
    String prefix = routePrefix.getValue();
    ResponseEntity<String> response = restTemplate.exchange(SLASH_ROUTES_STATIC + "/" + prefix, HttpMethod.DELETE, httpEntity,
        String.class);
    return response;
  }

  private Collection<Route> getRoutes() throws Exception {
    final HttpEntity requestBody = new HttpEntity(authHeaders());
    ResponseEntity<String> resp = restTemplate.exchange(SLASH_ROUTES, HttpMethod.GET, requestBody, String.class);
    String body = resp.getBody();
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    String routeList = objectMapper.readTree(body).at("/_embedded/immutableRouteList").toString();
    return Optional.ofNullable(objectMapper.readValue(routeList, new TypeReference<List<Route>>() {})).orElse(Collections.emptyList());

  }

  private Collection<StaticRoute> getStaticRoutes() throws Exception {
    final HttpEntity requestBody = new HttpEntity(authHeaders());
    ResponseEntity<String> resp = restTemplate.exchange(SLASH_ROUTES_STATIC, HttpMethod.GET, requestBody, String.class);
    String body = resp.getBody();
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    String routeList = objectMapper.readTree(body).at("/_embedded/immutableStaticRouteList").toString();
    return Optional.ofNullable(objectMapper.readValue(routeList, new TypeReference<List<StaticRoute>>() {})).orElse(Collections.emptyList());
  }

}
