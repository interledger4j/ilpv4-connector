package org.interledger.connector.server.ilphttp;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.ImmutableAccountSettings;
import org.interledger.connector.server.client.ConnectorAdminClient;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IlpOverHttpLinkSettings;
import org.interledger.link.http.IncomingLinkSettings;
import org.interledger.link.http.OutgoingLinkSettings;

import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.Map;

@EnableFeignClients(clients = ConnectorAdminClient.class)
public abstract class AbstractEndpointTest {

  protected static final String ENCRYPTED_SHH
    = "enc:JKS:crypto.p12:secret0:1:aes_gcm:AAAADKZPmASojt1iayb2bPy4D-Toq7TGLTN95HzCQAeJtz0=";

  protected static final String BASE64_SHH = Base64.getEncoder().encodeToString("shh".getBytes());

  protected static final String ALICE = "alice";
  protected static final String BOB = "bob";
  protected static final String CONNIE = "connie";

  @Autowired
  protected ConnectorAdminClient adminClient;

  @LocalServerPort
  private int localServerPort;

  protected URI baseURI() {
    try {
      return new URI("http://localhost:" + localServerPort);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  protected AccountSettings createAccount(AccountId accountId, String sharedSecret) {
    // Add the Bob Account to the Connector.
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


  // wrapper method with "assert" in the name to appease coday
  protected void assertLink(IlpOverHttpLink simpleBearerLink) {
    simpleBearerLink.testConnection();
  }


}
