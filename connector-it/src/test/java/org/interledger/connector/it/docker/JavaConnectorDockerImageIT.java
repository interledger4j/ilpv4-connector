package org.interledger.connector.it.docker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.connector.accounts.sub.LocalDestinationAddressUtils.PING_ACCOUNT_ID;

import org.interledger.connector.client.ConnectorAdminClient;
import org.interledger.connector.it.ContainerHelper;
import org.interledger.connector.it.markers.DockerImage;

import okhttp3.HttpUrl;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

import java.util.Optional;

/**
 * Test to validate the docker container can start up with the default dev profile
 */
@Category(DockerImage.class)
public class JavaConnectorDockerImageIT {

  private static final Logger LOGGER = LoggerFactory.getLogger(JavaConnectorDockerImageIT.class);
  private static final String containerVersion = Optional.ofNullable(System.getProperty("container.version"))
    .orElse("nightly");

  private static GenericContainer connector =
    ContainerHelper.javaConnector(containerVersion, Optional.of(LOGGER));
  private ConnectorAdminClient connectorAdminClient;

  @BeforeClass
  public static void startTopology() {
    connector.start();
  }

  @AfterClass
  public static void stopTopology() {
    connector.stop();
  }

  @Before
  public void setUp() {
    connectorAdminClient = ConnectorAdminClient.construct(getConnectorBaseUrl(), template -> {
      template.header("Authorization", "Basic YWRtaW46cGFzc3dvcmQ=");
    });
  }

  private HttpUrl getConnectorBaseUrl() {
    return new HttpUrl.Builder()
      .scheme("http")
      .host(connector.getContainerIpAddress())
      .port(connector.getFirstMappedPort())
      .build();
  }

  @Test
  public void basicAPI()  {
    assertThat(connectorAdminClient.findAccount(PING_ACCOUNT_ID.value())).isPresent();
  }

}
