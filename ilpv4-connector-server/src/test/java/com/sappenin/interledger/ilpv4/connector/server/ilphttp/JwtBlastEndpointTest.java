package com.sappenin.interledger.ilpv4.connector.server.ilphttp;

import com.sappenin.interledger.ilpv4.connector.server.ConnectorServerConfig;
import okhttp3.HttpUrl;
import org.interledger.connector.link.blast.BlastHttpSender;
import org.interledger.connector.link.blast.JwtBlastHttpSender;
import org.interledger.core.InterledgerAddress;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.autoconfigure.web.server.LocalManagementPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static com.sappenin.interledger.ilpv4.connector.server.spring.settings.blast.BlastConfig.BLAST;
import static com.sappenin.interledger.ilpv4.connector.server.spring.settings.properties.ConnectorProperties.BLAST_ENABLED;

/**
 * Ensures that the API endpoints for BLAST (i.e., `/ilp`) returns the correct values.
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
    "blast.outgoing.auth_type=JWT"
  }
)
public class JwtBlastEndpointTest {

  @LocalServerPort
  int randomServerPort;

  @LocalManagementPort
  int randomManagementPort;

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
    final BlastHttpSender blastHttpSender = jwtBlastHttpSender();
    blastHttpSender.testConnection();
  }

  private BlastHttpSender jwtBlastHttpSender() {
    return new JwtBlastHttpSender(
      () -> Optional.of(InterledgerAddress.of("example.blastClient")),
      HttpUrl.parse(template.getRootUri() + "/ilp").uri(),
      blastRestTemplate,
      () -> "bob",
      () -> HttpUrl.parse("https://alice.example.com/"),
      () -> "12345678912345678912345678912345" .getBytes()
    );
  }

}