package org.interledger.connector.server.ilpoverhttp;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.server.client.ConnectorAdminTestClient;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IlpOverHttpLinkSettings;
import org.interledger.link.http.IncomingLinkSettings;
import org.interledger.link.http.OutgoingLinkSettings;

import com.google.common.collect.Maps;
import feign.RequestInterceptor;
import feign.auth.BasicAuthRequestInterceptor;
import okhttp3.HttpUrl;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.LocalServerPort;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public abstract class AbstractEndpointTest {

  protected static final String ENCRYPTED_SHH
    = "enc:JKS:crypto.p12:secret0:1:aes_gcm:AAAADKZPmASojt1iayb2bPy4D-Toq7TGLTN95HzCQAeJtz0=";

  protected static final String ALICE = "alice";
  protected static final String BOB = "bob";
  protected static final String CONNIE = "connie";

  protected ConnectorAdminTestClient adminApiTestClient;

  @Value("${interledger.connector.adminPassword}")
  private String adminPassword;

  @LocalServerPort
  private int localServerPort;

  @Before
  public final void setUpAbstractEndpointTest() {
    final HttpUrl baseHttpUrl = HttpUrl.parse("http://localhost:" + localServerPort);
    final RequestInterceptor basicAuthRequestInterceptor = new BasicAuthRequestInterceptor("admin", adminPassword);
    adminApiTestClient = ConnectorAdminTestClient.construct(baseHttpUrl, basicAuthRequestInterceptor);
  }

  /**
   * To test the local connector using Feign, the baseURI has to be provide after injection so that the random local
   * server port is known
   *
   * @return base uri for the local connector
   */
  protected URI baseURI() {
    try {
      return new URI("http://localhost:" + localServerPort);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
  }

  protected AccountSettings createAccount(AccountId accountId, String sharedSecret) {
    // Add the Bob Account to the Connector.
    final Map<String, Object> customSettings = customSettings(sharedSecret);

    final AccountSettings accountSettings = AccountSettings.builder()
      .accountId(accountId)
      .description("HTTP account for Bob using a simple shared-secret")
      .accountRelationship(AccountRelationship.PEER)
      .linkType(IlpOverHttpLink.LINK_TYPE)
      .customSettings(customSettings)
      .assetScale(2)
      .assetCode("XRP")
      .build();

    return adminApiTestClient.createAccount(accountSettings);
  }

  protected AccountSettings updateSharedSecret(AccountSettings settings, String newSharedSecret) {
    AccountSettings toUpdate = AccountSettings.builder().from(settings)
      .customSettings(customSettings(newSharedSecret))
      .build();
    return adminApiTestClient.updateAccount(settings.accountId().value(), toUpdate);
  }

  private Map<String, Object> customSettings(String sharedSecret) {
    final Map<String, Object> customSettings = Maps.newHashMap();
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE, IlpOverHttpLinkSettings.AuthType.SIMPLE.name());
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_TOKEN_ISSUER, "https://bob.example.com/");
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_TOKEN_AUDIENCE, "https://connie.example.com/");
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_SHARED_SECRET, sharedSecret);

    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_AUTH_TYPE, IlpOverHttpLinkSettings.AuthType.SIMPLE.name());
    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_ISSUER, "https://connie.example.com/");
    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_AUDIENCE, "https://bob.example.com/");
    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_SUBJECT, "connie");
    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_SHARED_SECRET, sharedSecret);
    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_URL, "https://bob.example.com");
    return customSettings;
  }

  // wrapper method with "assert" in the name to appease coday
  protected void assertLink(IlpOverHttpLink simpleBearerLink) {
    simpleBearerLink.testConnection();
  }


}
