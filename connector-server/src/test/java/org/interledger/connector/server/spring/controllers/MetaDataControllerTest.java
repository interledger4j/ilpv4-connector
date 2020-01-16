package org.interledger.connector.server.spring.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.server.ConnectorServerConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.boot.test.json.JsonContentAssert;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  classes = {ConnectorServerConfig.class}
)
@ActiveProfiles("test")
public class MetaDataControllerTest {

  private Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  private TestRestTemplate restTemplate;

  private BasicJsonTester jsonTester = new BasicJsonTester(getClass());

  @Value("${interledger.connector.nodeIlpAddress}")
  private String testConnectorAddress;

  @Test
  public void getConnectorMetaData() {
    ResponseEntity<String> metaDataResponse = restTemplate.getForEntity(PathConstants.SLASH, String.class);

    logger.info("metaDataResponse: " + metaDataResponse.getBody());

    JsonContentAssert assertJson = assertThat(jsonTester.from(metaDataResponse.getBody()));
    assertJson.extractingJsonPathValue("ilp_address").isEqualTo(testConnectorAddress);
    assertJson.hasJsonPath("version");
  }

}