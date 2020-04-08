package org.interledger.connector.server.wallet.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.interledger.connector.server.spring.settings.link.IlpOverHttpConfig.ILP_OVER_HTTP;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.server.ConnectorServerConfig;
import org.interledger.connector.server.ilpoverhttp.AbstractEndpointTest;
import org.interledger.core.InterledgerAddress;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.spsp.StreamConnectionDetails;
import org.interledger.spsp.client.SimpleSpspClient;
import org.interledger.spsp.client.SpspClient;
import org.interledger.spsp.client.SpspClientException;
import org.interledger.stream.Denomination;
import org.interledger.stream.SendMoneyRequest;
import org.interledger.stream.SendMoneyResult;
import org.interledger.stream.sender.FixedSenderAmountPaymentTracker;
import org.interledger.stream.sender.SimpleStreamSender;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.primitives.UnsignedLong;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Unit test that excercises the SPSP Controller when SPSP is configured to run with an empty spsp path (e.g., for
 * payment pointers like `$example.com/user`).
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  classes = {ConnectorServerConfig.class},
  properties = {"interledger.connector.spsp.urlPath="}
)
@ActiveProfiles( {"test"}) // Uses the `application-test.properties` file in the `src/test/resources` folder
public class SpspControllerWithNoPathSpringTest extends AbstractEndpointTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @LocalServerPort
  private int randomServerPort;

  @Autowired
  @Qualifier(ILP_OVER_HTTP)
  private OkHttpClient okHttpClient;

  @Autowired
  private ObjectMapper objectMapper;

  private SpspClient spspClient;

  @Before
  public void setUp() {
    spspClient = new SimpleSpspClient();
  }

  @Test
  public void testEmptyPath() {
    expectedException.expect(SpspClientException.class);
    expectedException.expectMessage(
      startsWith("Received non-successful HTTP response code 400 calling http://localhost:")
    );

    spspClient.getStreamConnectionDetails(HttpUrl.parse("http://localhost:" + randomServerPort));
  }

  @Test
  public void testNormalPath() {
    final StreamConnectionDetails result = spspClient.getStreamConnectionDetails(
      HttpUrl.parse("http://localhost:" + randomServerPort).newBuilder()
        .addPathSegment("bob")
        .build());

    assertThat(result.destinationAddress().startsWith("test1.connie.bob"));
    assertThat(result.sharedSecret()).isNotNull();
  }

  /**
   * Even though this path is technicaly invalid, it still resolved because the current implementation accepts
   * additional paths in the payment pointer.
   */
  @Test
  public void testAdditionalPath() {
    final StreamConnectionDetails result = spspClient.getStreamConnectionDetails(
      HttpUrl.parse("http://localhost:" + randomServerPort).newBuilder()
        .addPathSegment("p") // errant /p
        .addPathSegment("bob")
        .build()
    );

    assertThat(result.destinationAddress().startsWith("test1.connie.p.bob"));
    assertThat(result.sharedSecret()).isNotNull();
  }

  /**
   * When the SPSP path is empty, there shouldn't be collisions on URL paths (e.g., /accounts) if the correct SPSP
   * header is supplied. Additionally, security should not reject such a request. This test validates that this is
   * working properly.
   */
  @Test
  public void testNormalPathWithAccountsAccountId() {
    final StreamConnectionDetails result = spspClient.getStreamConnectionDetails(
      HttpUrl.parse("http://localhost:" + randomServerPort).newBuilder()
        .addPathSegment("accounts") // Simulate an SPSP userId of `accounts` which clashes with the admin API.
        .build());

    assertThat(result.destinationAddress().startsWith("test1.connie.accounts"));
    assertThat(result.sharedSecret()).isNotNull();
  }

  /**
   * Validate that Bob can send a packet to `alice` and get a fulfillment back. Since local spsp fulfillment is enabled,
   * this should succeed.
   */
  @Test
  public void bobPaysAliceUsingLocalFulfillment() throws InterruptedException, ExecutionException, TimeoutException {
    String authToken = "shh";
    AccountSettings bob = createAccount(AccountId.of(BOB + UUID.randomUUID()), customSettingsSimple(authToken));
    AccountSettings alice =
      createAccount(AccountId.of(ALICE + UUID.randomUUID()), customSettingsSimple(authToken));
    final IlpOverHttpLink ilpOverHttpLink = linkToServer(bob.accountId(), authToken, okHttpClient, objectMapper);

    final StreamConnectionDetails connectionDetails = spspClient.getStreamConnectionDetails(
      HttpUrl.parse("http://localhost:" + randomServerPort).newBuilder()
        .addPathSegment(alice.accountId().value())
        .build()
    );

    SimpleStreamSender simpleStreamSender = new SimpleStreamSender(ilpOverHttpLink);

    UnsignedLong amountToSend = UnsignedLong.valueOf(1000);
    SendMoneyResult sendResult = simpleStreamSender.sendMoney(
      SendMoneyRequest.builder()
        .sharedSecret(connectionDetails.sharedSecret())
        .destinationAddress(connectionDetails.destinationAddress())
        .sourceAddress(InterledgerAddress.of("test.foo"))
        .amount(amountToSend)
        .paymentTracker(new FixedSenderAmountPaymentTracker(amountToSend))
        .denomination(Denomination.builder().assetCode(bob.assetCode()).assetScale((short) bob.assetScale()).build())
        .requestId(UUID.randomUUID())
        .build()
    ).get(10, TimeUnit.SECONDS);

    assertThat(sendResult.successfulPayment()).isTrue();
  }

}
