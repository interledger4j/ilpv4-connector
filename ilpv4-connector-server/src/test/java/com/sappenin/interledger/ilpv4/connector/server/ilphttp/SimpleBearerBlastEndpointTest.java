package com.sappenin.interledger.ilpv4.connector.server.ilphttp;

import com.google.common.collect.Maps;
import com.sappenin.interledger.ilpv4.connector.server.ConnectorServerConfig;
import com.sappenin.interledger.ilpv4.connector.server.spring.settings.properties.ConnectorProperties;
import okhttp3.HttpUrl;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.link.blast.BlastHttpSender;
import org.interledger.connector.link.blast.BlastLink;
import org.interledger.connector.link.blast.BlastLinkSettings;
import org.interledger.connector.link.blast.ImmutableOutgoingLinkSettings;
import org.interledger.connector.link.blast.IncomingLinkSettings;
import org.interledger.connector.link.blast.OutgoingLinkSettings;
import org.interledger.connector.link.blast.SimpleBearerBlastHttpSender;
import org.interledger.core.InterledgerAddress;
import org.interledger.crypto.Decryptor;
import org.interledger.ilpv4.connector.persistence.entities.AccountSettingsEntity;
import org.interledger.ilpv4.connector.persistence.repositories.AccountSettingsRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

import static com.sappenin.interledger.ilpv4.connector.server.spring.settings.blast.BlastConfig.BLAST;
import static org.interledger.crypto.CryptoConfigConstants.ILPV4_CONNECTOR_KEYSTORE_JKS_FILENAME;
import static org.interledger.crypto.CryptoConfigConstants.ILPV4_CONNECTOR_KEYSTORE_JKS_PASSWORD;
import static org.interledger.crypto.CryptoConfigConstants.ILPV4_CONNECTOR_KEYSTORE_JKS_SECRET0_ALIAS;
import static org.interledger.crypto.CryptoConfigConstants.ILPV4_CONNECTOR_KEYSTORE_JKS_SECRET0_PASSWORD;

/**
 * Ensures that the API endpoints for BLAST (i.e., `/ilp`) returns the correct values when a
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  classes = {ConnectorServerConfig.class}
)
@ActiveProfiles({"test"}) // Maps to `application-test.yml`
@TestPropertySource(
  properties = {
    ConnectorProperties.ENABLED_PROTOCOLS + "." + ConnectorProperties.BLAST_ENABLED + "=true",
    ILPV4_CONNECTOR_KEYSTORE_JKS_FILENAME + "=crypto/crypto.p12",
    ILPV4_CONNECTOR_KEYSTORE_JKS_PASSWORD + "=password",
    ILPV4_CONNECTOR_KEYSTORE_JKS_SECRET0_ALIAS + "=secret0",
    ILPV4_CONNECTOR_KEYSTORE_JKS_SECRET0_PASSWORD + "=password",
  }
)
public class SimpleBearerBlastEndpointTest {

  @LocalServerPort
  int randomServerPort;

  @Autowired
  @Qualifier(BLAST)
  RestTemplate blastRestTemplate;

  @Autowired
  Decryptor decryptor;

  @Autowired
  TestRestTemplate template;

  @Autowired
  AccountSettingsRepository accountSettingsRepository;

  @Before
  public void setup() {
    // Add the Bob Account to the Connector.


    //    id: bob-jwt
    //    description: BLAST account for Bob
    //        # The default relationship-type that this connector should view an account as.
    //    relationship: PEER
    //        # Type of the Link that should be used for this account.
    //      linkType: BlastLink
    //        # Whether we should broadcast routes to this peer. Defaults to false for relation=child and true otherwise.
    //      sendRoutes: false
    //        # Whether we should receive and process route broadcasts fromEncodedValue this peer. Defaults to false for relation=child and true otherwise.
    //      receiveRoutes: false
    //    customSettings:


    //          #Incoming
    //    blast.incoming.auth_type: JWT_HS_256
    //    blast.incoming.auth_subject: bob # The accountId held at this connector.
    //          # Maps to `shh` using the crypto/crypto.p12 JKS
    //    blast.incoming.auth_credential: enc:JKS:crypto.p12:secret0:1:aes_gcm:AAAADKZPmASojt1iayb2bPy4D-Toq7TGLTN95HzCQAeJtz0=
    //      blast.incoming.token_issuer: https://bob.example.com # The JWT_HS_256 issuer of the incoming token.
    //    blast.incoming.url: http://localhost:8080/ilp


    //          # Outgoing
    //    blast.outgoing.auth_type: JWT_HS_256
    //    blast.outgoing.auth_subject: alice # The accountId held at the remote connector.
    //      blast.outgoing.auth_credential: unused
    //    blast.outgoing.token_issuer: https://alice.example.com # The JWT_HS_256 issuer of the outgoing token.
    //    blast.outgoing.token_expiry: PT2M # See Duration.java for structure.
    //      blast.outgoing.url: http://localhost:8081/ilp

    final Map<String, Object> customSettings = Maps.newHashMap();
    customSettings.put(IncomingLinkSettings.BLAST_INCOMING_AUTH_TYPE, BlastLinkSettings.AuthType.JWT_HS_256.name());
    customSettings.put(IncomingLinkSettings.BLAST_INCOMING_TOKEN_AUDIENCE, "https://alice.example.com/");
    customSettings.put(IncomingLinkSettings.BLAST_INCOMING_TOKEN_ISSUER, HttpUrl.parse("https://bob.example.com/"));
    customSettings.put(IncomingLinkSettings.BLAST_INCOMING_TOKEN_SUBJECT, "bob-simple");
    customSettings.put(IncomingLinkSettings.BLAST_INCOMING_SHARED_SECRET,
      "enc:JKS:crypto.p12:secret0:1:aes_gcm:AAAADKZPmASojt1iayb2bPy4D-Toq7TGLTN95HzCQAeJtz0=");

    final AccountSettings accountSettings = AccountSettings.builder()
      .accountId(AccountId.of("bob-simple"))
      .description("BLAST account for Bob using a simple shared-secret")
      .accountRelationship(AccountRelationship.PEER)
      .linkType(BlastLink.LINK_TYPE)
      .customSettings(customSettings)
      .assetScale(2)
      .assetCode("XRP")
      .build();
    accountSettingsRepository.save(new AccountSettingsEntity(accountSettings));
  }

  /**
   * Validate the "test connection" method in the IL-DCP requestor.
   */
  @Test
  public void ildcpTestConnection() {
    final BlastHttpSender blastHttpSender = simpleBearerBlastHttpSender();
    blastHttpSender.testConnection();
  }

  private BlastHttpSender simpleBearerBlastHttpSender() {

    final OutgoingLinkSettings outgoingLinkSettings = ImmutableOutgoingLinkSettings.builder()
      .authType(BlastLinkSettings.AuthType.JWT_HS_256)
      .tokenSubject("bob-simple")
      .tokenIssuer(HttpUrl.parse("https://bob.example.com/"))
      .tokenAudience("https://alice.example.com/")
      .url(HttpUrl.parse(template.getRootUri() + "/ilp"))
      // The is the encrypted variant of `shh`
      .encryptedTokenSharedSecret(
        "enc:JKS:crypto.p12:secret0:1:aes_gcm:AAAADKZPmASojt1iayb2bPy4D-Toq7TGLTN95HzCQAeJtz0=")
      .build();

    // Assemble a JWT_HS_256 Bearer token that can be supplied directly to the BlastSender.
    //    final String bearerToken = JWT.create()
    //      .withIssuedAt(new Date())
    //      .withIssuer("https://alice.example.com/")
    //      .withSubject("bob") // account identifier at the remote server.
    //      .withAudience("https://bob.exmaple.com")
    //      .sign(Algorithm.HMAC256("12345678912345678912345678912345"));

    return new SimpleBearerBlastHttpSender(
      () -> Optional.of(InterledgerAddress.of("example.blastClient")),
      blastRestTemplate,
      outgoingLinkSettings,
      decryptor
    );
  }

}