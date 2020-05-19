package org.interledger.connector.server.wallet.controllers;

import static org.hamcrest.Matchers.startsWith;

import org.interledger.connector.server.ConnectorServerConfig;
import org.interledger.spsp.client.SimpleSpspClient;
import org.interledger.spsp.client.SpspClient;
import org.interledger.spsp.client.SpspClientException;

import okhttp3.HttpUrl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  classes = {ConnectorServerConfig.class},
  properties = {"interledger.connector.enabledProtocols.spspEnabled=false",
    "interledger.connector.enabledProtocols.openPaymentsEnabled=false"}
)
@ActiveProfiles( {"test"}) // Uses the `application-test.properties` file in the `src/test/resources` folder
public class SpspControllerDisabledSpringTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @LocalServerPort
  private int randomServerPort;

  private SpspClient spspClient;

  @Before
  public void setUp() {
    spspClient = new SimpleSpspClient();
  }

  @Test
  public void testEmptyPath() {
    expectedException.expect(SpspClientException.class);
    expectedException.expectMessage(
      startsWith("Received non-successful HTTP response code 406 calling http://localhost:")
    );

    spspClient.getStreamConnectionDetails(HttpUrl.parse("http://localhost:" + randomServerPort));
  }

  @Test
  public void testInvalidPath() {
    expectedException.expect(SpspClientException.class);
    expectedException
      .expectMessage(startsWith("Received non-successful HTTP response code 401 calling http://localhost:"));

    spspClient.getStreamConnectionDetails(
      HttpUrl.parse("http://localhost:" + randomServerPort).newBuilder()
        .addPathSegment("bob") // Missing /p
        .build()
    );
  }

  @Test
  public void testNormalPath() {
    expectedException.expect(SpspClientException.class);
    expectedException.expectMessage(
      startsWith("Received non-successful HTTP response code 401 calling http://localhost:")
    );

    spspClient.getStreamConnectionDetails(
      HttpUrl.parse("http://localhost:" + randomServerPort).newBuilder()
        .addPathSegment("p")
        .addPathSegment("bob")
        .build());
  }

  /**
   * Even though this path is technicaly invalid, it still resolved because the current implementation accepts
   * additional paths in the payment pointer.
   */
  @Test
  public void testAdditionalPath() {
    expectedException.expect(SpspClientException.class);
    expectedException.expectMessage(
      startsWith("Received non-successful HTTP response code 401 calling http://localhost:")
    );

    spspClient.getStreamConnectionDetails(
      HttpUrl.parse("http://localhost:" + randomServerPort).newBuilder()
        .addPathSegment("p")
        .addPathSegment("bob")
        .addPathSegment("foo") // extra path
        .build()
    );
  }

  /**
   * When the SPSP path is empty, there shouldn't be collisions on URL paths (e.g., /accounts) if the correct SPSP
   * header is supplied. Additionally, security should not reject such a request. This test validates that this is
   * working properly.
   */
  @Test
  public void testAccountsPath() {
    expectedException.expect(SpspClientException.class);
    expectedException
      .expectMessage(startsWith("Received non-successful HTTP response code 401 calling http://localhost:"));

    spspClient.getStreamConnectionDetails(
      HttpUrl.parse("http://localhost:" + randomServerPort).newBuilder()
        .addPathSegment("accounts") // Simulate an SPSP userId of `accounts` which clashes with the admin API.
        .build());
  }

}
