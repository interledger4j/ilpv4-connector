package org.interledger.connector.server.ilpoverhttp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.connector.server.spring.settings.link.IlpOverHttpConfig.ILP_OVER_HTTP;
import static org.interledger.connector.server.spring.settings.web.JacksonConfig.PROBLEM;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.interledger.codecs.ilp.InterledgerCodecContextFactory;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.packetswitch.ILPv4PacketSwitch;
import org.interledger.connector.server.ConnectorServerConfig;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.link.LinkId;
import org.interledger.link.exceptions.LinkException;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.auth.SimpleBearerTokenSupplier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.primitives.UnsignedLong;
import okhttp3.OkHttpClient;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.zalando.problem.Status;
import org.zalando.problem.StatusType;
import org.zalando.problem.ThrowableProblem;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

/**
 * Ensures that the API endpoints for ILP-over-HTTP (i.e., `/ilp`) return the correct values for various test scenarios
 * using * the `SIMPLE` profile defined in the Interledger HTTP Authentication recommendations RFC.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {ConnectorServerConfig.class}
)
@ActiveProfiles( {"test"}) // Uses the `application-test.properties` file in the `src/test/resources` folder
public class SimpleIlpOverHttpEndpointTest extends AbstractEndpointTest {

  private static final String BAD_SECRET = Base64.getEncoder().encodeToString("pfft".getBytes());

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @MockBean
  private ILPv4PacketSwitch ilpPacketSwitch;

  @Autowired
  @Qualifier(ILP_OVER_HTTP)
  private OkHttpClient okHttpClient;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  @Qualifier(PROBLEM)
  private ObjectMapper problemObjectMapper;

  @Autowired
  private TestRestTemplate template;

  /**
   * Validate the "test connection" method in the IL-DCP requestor created with an encrypted secret.
   */
  @Test
  public void ildcpTestConnectionWithEncryptedSecret() {
    String accountId = "bob_ross";
    createAccount(AccountId.of(accountId), customSettingsSimple(ENCRYPTED_SHH));
    final IlpOverHttpLink simpleBearerLink = simpleBearerLink(accountId, "shh", objectMapper);
    simpleBearerLink.setLinkId(LinkId.of(accountId));
    assertLink(simpleBearerLink);
  }

  /**
   * Validate the "test connection" method in the IL-DCP requestor created with an base64 encoded secret.
   */
  @Test
  public void ildcpTestConnectionWithPlainTextSecret() {
    String accountId = "bob_marley";
    String shh = "shh";
    AccountSettings settings = createAccount(AccountId.of(accountId), customSettingsSimple(shh));
    final IlpOverHttpLink simpleBearerLink = simpleBearerLink(accountId, shh, objectMapper);
    simpleBearerLink.setLinkId(LinkId.of(accountId));
    assertLink(simpleBearerLink);

    String encrypted_oh_hi =
      "enc:JKS:crypto.p12:secret0:1:aes_gcm:AAAADJ6Y6dG3Jp_PoeamK7Q4F5b-yuihjvEGejjIbYs68JLzHttuDCds";
    String plain_text_oh_hi = "oh hi mark";

    updateSimpleAuthToken(settings, plain_text_oh_hi);
    // send payment with new credentials
    final IlpOverHttpLink anotherBearerLink = simpleBearerLink(accountId, plain_text_oh_hi, objectMapper);
    anotherBearerLink.setLinkId(LinkId.of(accountId));
    assertLink(anotherBearerLink);
  }

  /**
   * Validate the "test connection" method in the IL-DCP requestor.
   */
  @Test
  public void incorrectTokenCredentials() {
    String accountId = "alice_cooper";
    createAccount(AccountId.of(accountId), customSettingsSimple("shh"));
    final IlpOverHttpLink simpleBearerLink = simpleBearerLink(accountId, BAD_SECRET, objectMapper);
    simpleBearerLink.setLinkId(LinkId.of(accountId));
    expectedException.expect(LinkException.class);
    expectedException.expectMessage("Unauthorized");
    assertLink(simpleBearerLink);
  }

  @Test
  public void parseThrowableProblemDoesNotLogError() {
    String accountId = "trouble_maker";
    createAccount(AccountId.of(accountId), customSettingsSimple("shh"));
    final IlpOverHttpLink simpleBearerLink = simpleBearerLink(accountId, "shh", problemObjectMapper);
    simpleBearerLink.setLinkId(LinkId.of(accountId));

    when(ilpPacketSwitch.switchPacket(any(), any())).thenThrow(new ThrowableProblem() {

      @Override
      public String getTitle() {
        return "Internal Error";
      }

      @Override
      public StatusType getStatus() {
        return Status.INTERNAL_SERVER_ERROR;
      }
    });

    InterledgerPreparePacket preparePacket = InterledgerPreparePacket.builder()
      .destination(InterledgerAddress.of("test.connie.vic"))
      .amount(UnsignedLong.ONE)
      .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
      .executionCondition(InterledgerCondition.of(new byte[32]))
      .build();

    InterledgerResponsePacket responsePacket = simpleBearerLink.sendPacket(preparePacket);
    assertThat(responsePacket).isInstanceOf(InterledgerRejectPacket.class);
    assertThat(((InterledgerRejectPacket) responsePacket).getCode()).isEqualTo(InterledgerErrorCode.T00_INTERNAL_ERROR);
    assertThat(((InterledgerRejectPacket) responsePacket).getMessage()).isEqualTo("Internal Error");
  }

  private IlpOverHttpLink simpleBearerLink(String accountId, String bearerToken, ObjectMapper objectMapper) {
    return new IlpOverHttpLink(
      () -> InterledgerAddress.of("test.bob"),
      createAccountIlpUrl(template.getRootUri(), AccountId.of(accountId)),
      okHttpClient,
      objectMapper,
      InterledgerCodecContextFactory.oer(),
      new SimpleBearerTokenSupplier(bearerToken)
    );

  }

}
