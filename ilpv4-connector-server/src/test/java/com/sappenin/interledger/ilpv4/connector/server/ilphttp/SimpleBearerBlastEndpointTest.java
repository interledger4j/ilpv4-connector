package com.sappenin.interledger.ilpv4.connector.server.ilphttp;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.sappenin.interledger.ilpv4.connector.server.ConnectorServerConfig;
import okhttp3.HttpUrl;
import org.interledger.connector.link.blast.BlastHttpSender;
import org.interledger.connector.link.blast.SimpleBearerBlastHttpSender;
import org.interledger.core.InterledgerAddress;
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

import java.util.Date;
import java.util.Optional;

import static com.sappenin.interledger.ilpv4.connector.server.spring.settings.blast.BlastConfig.BLAST;
import static com.sappenin.interledger.ilpv4.connector.server.spring.settings.properties.ConnectorProperties.BLAST_ENABLED;
import static org.interledger.connector.link.blast.BlastHeaders.BLAST_AUDIENCE;

/**
 * Ensures that the API endpoints for BLAST (i.e., `/ilp`) returns the correct values when a
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  classes = {ConnectorServerConfig.class}
)
@ActiveProfiles({"test"})
@TestPropertySource(
  properties = {
    BLAST_ENABLED + "=true",
    "blast.outgoing.auth_type=SIMPLE"
  }
)
public class SimpleBearerBlastEndpointTest {

  @LocalServerPort
  int randomServerPort;

  @Autowired
  @Qualifier(BLAST)
  RestTemplate blastRestTemplate;

  @Autowired
  private TestRestTemplate template;

  /**
   * Validate the "test connection" method in the IL-DCP requestor.
   */
  @Test
  public void ildcpTestConnection() {
    final BlastHttpSender blastHttpSender = simpleBearerBlastHttpSender();
    blastHttpSender.testConnection();
  }

  private BlastHttpSender simpleBearerBlastHttpSender() {
    // Assemble a JWT Bearer token that can be supplied directly to the BlastSender.
    final String bearerToken = JWT.create()
      .withIssuedAt(new Date())
      .withIssuer("https://alice.example.com/")
      .withSubject("bob") // account identifier at the remote server.
      .withAudience(BLAST_AUDIENCE)
      .sign(Algorithm.HMAC256("12345678912345678912345678912345"));

    return new SimpleBearerBlastHttpSender(
      () -> Optional.of(InterledgerAddress.of("example.blastClient")),
      HttpUrl.parse(template.getRootUri() + "/ilp").uri(),
      blastRestTemplate,
      () -> "bob",
      () -> bearerToken.getBytes()
    );
  }

}