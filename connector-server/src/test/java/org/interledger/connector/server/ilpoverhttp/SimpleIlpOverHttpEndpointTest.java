package org.interledger.connector.server.ilpoverhttp;

import static org.interledger.connector.server.spring.settings.link.IlpOverHttpConfig.ILP_OVER_HTTP;

import org.interledger.codecs.ilp.InterledgerCodecContextFactory;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.server.ConnectorServerConfig;
import org.interledger.core.InterledgerAddress;
import org.interledger.link.LinkId;
import org.interledger.link.exceptions.LinkException;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IlpOverHttpLinkSettings;
import org.interledger.link.http.IncomingLinkSettings;
import org.interledger.link.http.OutgoingLinkSettings;
import org.interledger.link.http.auth.SimpleBearerTokenSupplier;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

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

  @Autowired
  @Qualifier(ILP_OVER_HTTP)
  private OkHttpClient okHttpClient;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private TestRestTemplate template;

  /**
   * Validate the "test connection" method in the IL-DCP requestor created with an encrypted secret.
   */
  @Test
  public void ildcpTestConnectionWithEncryptedSecret() {
    String accountId = "bob_ross";
    createAccount(AccountId.of(accountId), ENCRYPTED_SHH);
    final IlpOverHttpLink simpleBearerLink = simpleBearerLink(ENCRYPTED_SHH, accountId + ":shh");
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
    AccountSettings settings = createAccount(AccountId.of(accountId), shh);
    final IlpOverHttpLink simpleBearerLink = simpleBearerLink(ENCRYPTED_SHH, accountId + ":" + shh);
    simpleBearerLink.setLinkId(LinkId.of(accountId));
    assertLink(simpleBearerLink);

    String encrypted_oh_hi =
      "enc:JKS:crypto.p12:secret0:1:aes_gcm:AAAADJ6Y6dG3Jp_PoeamK7Q4F5b-yuihjvEGejjIbYs68JLzHttuDCds";
    String plain_text_oh_hi = "oh hi mark";

    updateSharedSecret(settings, plain_text_oh_hi);
    // send payment with new credentials
    final IlpOverHttpLink anotherBearerLink = simpleBearerLink(encrypted_oh_hi, accountId + ":" + plain_text_oh_hi);
    anotherBearerLink.setLinkId(LinkId.of(accountId));
    assertLink(anotherBearerLink);
  }

  /**
   * Validate the "test connection" method in the IL-DCP requestor.
   */
  @Test
  public void incorrectTokenCredentials() {
    String accountId = "alice_cooper";
    createAccount(AccountId.of(accountId), "shh");
    final IlpOverHttpLink simpleBearerLink = simpleBearerLink(ENCRYPTED_SHH, accountId + ":" + BAD_SECRET);
    simpleBearerLink.setLinkId(LinkId.of(accountId));
    expectedException.expect(LinkException.class);
    expectedException.expectMessage("Unauthorized");
    assertLink(simpleBearerLink);
  }

  private IlpOverHttpLink simpleBearerLink(String sharedSecret, String bearerToken) {

    final OutgoingLinkSettings outgoingLinkSettings = OutgoingLinkSettings.builder()
      .authType(IlpOverHttpLinkSettings.AuthType.SIMPLE)
      .tokenSubject("bob")
      .tokenIssuer(HttpUrl.parse("https://bob.example.com/"))
      .tokenAudience(HttpUrl.parse("https://n-a.example.com"))
      .url(HttpUrl.parse(template.getRootUri() + "/ilp"))
      // The is the encrypted variant of `shh`
      .encryptedTokenSharedSecret(sharedSecret)
      .build();

    final IncomingLinkSettings incomingLinkSettings = IncomingLinkSettings.builder()
      .encryptedTokenSharedSecret(sharedSecret)
      .authType(IlpOverHttpLinkSettings.AuthType.SIMPLE)
      .tokenIssuer(outgoingLinkSettings.tokenIssuer())
      .tokenAudience(outgoingLinkSettings.tokenAudience())
      .build();

    final IlpOverHttpLinkSettings linkSettings = IlpOverHttpLinkSettings.builder()
      .incomingHttpLinkSettings(incomingLinkSettings)
      .outgoingHttpLinkSettings(outgoingLinkSettings)
      .build();

    return new IlpOverHttpLink(
      () -> InterledgerAddress.of("test.bob"),
      linkSettings,
      okHttpClient,
      objectMapper,
      InterledgerCodecContextFactory.oer(),
      new SimpleBearerTokenSupplier(bearerToken)
    );

  }

}
