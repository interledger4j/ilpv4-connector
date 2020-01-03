package org.interledger.connector.server.ilpoverhttp;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.ImmutableAccountSettings;
import org.interledger.connector.server.client.ConnectorAdminClient;
import org.interledger.connector.server.client.ConnectorUserClient;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IncomingLinkSettings;
import org.interledger.link.http.JwtAuthSettings;
import org.interledger.link.http.OutgoingLinkSettings;

import com.google.common.collect.Maps;
import okhttp3.HttpUrl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

@EnableFeignClients(clients = { ConnectorAdminClient.class, ConnectorUserClient.class })
public abstract class AbstractEndpointTest {

  protected static final String ENCRYPTED_SHH
    = "enc:JKS:crypto.p12:secret0:1:aes_gcm:AAAADKZPmASojt1iayb2bPy4D-Toq7TGLTN95HzCQAeJtz0=";

  protected static final String ALICE = "alice";
  protected static final String BOB = "bob";
  protected static final String CONNIE = "connie";

  @Autowired
  protected ConnectorAdminClient adminClient;

  @Autowired
  protected ConnectorUserClient userClient;

  @LocalServerPort
  private int localServerPort;

  /**
   * To test the local connector using Feign, the baseURI has to be provide
   * after injection so that the random local server port is known
   * @return base uri for the local connector
   */
  protected URI baseURI() {
    try {
      return new URI("http://localhost:" + localServerPort);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
  }

  protected AccountSettings createAccount(AccountId accountId, Map<String, Object> customSettings) {
    final AccountSettings accountSettings = AccountSettings.builder()
      .accountId(accountId)
      .description("HTTP account for Bob using a simple shared-secret")
      .accountRelationship(AccountRelationship.PEER)
      .linkType(IlpOverHttpLink.LINK_TYPE)
      .customSettings(customSettings)
      .assetScale(2)
      .assetCode("XRP")
      .build();

    ResponseEntity<ImmutableAccountSettings> result = adminClient.createAccount(baseURI(), accountSettings);
    return result.getBody();
  }

  protected AccountSettings updateSimpleAuthToken(AccountSettings settings, String newSharedSecret) {
    AccountSettings toUpdate = AccountSettings.builder().from(settings)
      .customSettings(customSettingsSimple(newSharedSecret))
      .build();
    return adminClient.updateAccount(baseURI(), settings.accountId().value(), toUpdate);
  }


  protected Map<String, Object> customSettingsSimple(String authToken) {
    final Map<String, Object> customSettings = Maps.newHashMap();
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE, "SIMPLE");
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_SIMPLE_AUTH_TOKEN, authToken);

    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_AUTH_TYPE, "SIMPLE");
    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_SIMPLE_AUTH_TOKEN, authToken);
    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_URL, "https://bob.example.com");
    return customSettings;
  }

  protected Map<String, Object> customSettingsJwtHs256(String sharedSecret, String subject) {
    final Map<String, Object> customSettings = Maps.newHashMap();
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE, "JWT_HS_256");
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_SHARED_SECRET, sharedSecret);
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_TOKEN_SUBJECT, subject);

    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_AUTH_TYPE, "JWT_HS_256");
    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_SUBJECT, "connie");
    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_SHARED_SECRET, sharedSecret);
    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_URL, "https://bob.example.com");
    return customSettings;
  }

  protected Map<String, Object> customSettingsJwtRs256(JwtAuthSettings jwtAuthSettings) {
    final Map<String, Object> customSettings = Maps.newHashMap();
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE, "JWT_RS_256");
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_TOKEN_SUBJECT, jwtAuthSettings.tokenSubject());
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_TOKEN_ISSUER, jwtAuthSettings.tokenIssuer().get().toString());
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_TOKEN_AUDIENCE, jwtAuthSettings.tokenAudience().get());
    return customSettings;
  }

  // wrapper method with "assert" in the name to appease coday
  protected void assertLink(IlpOverHttpLink simpleBearerLink) {
    simpleBearerLink.testConnection();
  }


  protected HttpUrl createAccountIlpUrl(String rootUri, AccountId accountId) {
    return HttpUrl.parse(rootUri + "/accounts/" + accountId + "/ilp");
  }
}
