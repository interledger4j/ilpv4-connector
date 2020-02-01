package org.interledger.connector.server.spring.controllers;

import static org.interledger.connector.settlement.SettlementConstants.IDEMPOTENCY_KEY;

import org.interledger.connector.accounts.AccountManager;
import org.interledger.connector.links.LinkSettingsFactory;
import org.interledger.connector.packetswitch.ILPv4PacketSwitch;
import org.interledger.connector.persistence.repositories.AccountSettingsRepository;
import org.interledger.connector.routing.ExternalRoutingService;
import org.interledger.connector.server.spring.settings.ConnectorSettingsFromPropertyFileTest;
import org.interledger.connector.server.spring.settings.web.SpringConnectorWebMvc;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.connector.settlement.SettlementService;
import org.interledger.crypto.EncryptionService;
import org.interledger.link.LinkFactoryProvider;
import org.interledger.link.PacketRejector;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import io.prometheus.client.cache.caffeine.CacheMetricsCollector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.util.function.Supplier;

/**
 * An abstract super class for all Controller tests.
 */
@ContextConfiguration(classes = {
  ControllerTestConfig.class, // For custom Beans.
  SpringConnectorWebMvc.class,
  ConnectorSettingsFromPropertyFileTest.TestConfiguration.class
})
@ActiveProfiles( {"test"}) // Uses the `application-test.properties` file in the `src/test/resources` folder
public abstract class AbstractControllerTest {

  @Autowired
  protected ObjectMapper objectMapper;

  @MockBean
  protected SettlementService settlementServiceMock;

  @MockBean
  protected AccountManager accountManagerMock;

  @MockBean
  protected AccountSettingsRepository accountSettingsRepositoryMock;

  /*@MockBean
  protected ConnectorSettings connectorSettingsMock;*/

  /*@MockBean
  protected Supplier<ConnectorSettings> connectorSettingsSupplierMock;*/

  @MockBean
  protected EncryptionService encryptionServiceMock;

  @MockBean
  protected ExternalRoutingService externalRoutingServiceMock;

  @MockBean
  protected LinkSettingsFactory linkSettingsFactoryMock;

  @MockBean
  protected CacheMetricsCollector cacheMetricsCollectorMock;

  @MockBean
  protected PacketRejector packetRejectorMock;

  @MockBean
  protected LinkFactoryProvider linkFactoryProviderMock;

  @MockBean
  protected ILPv4PacketSwitch ilPv4PacketSwitchMock;

  @MockBean
  protected BuildProperties buildProperties;

  protected String asJsonString(final Object obj) throws JsonProcessingException {
    return this.objectMapper.writeValueAsString(obj);
  }

  /**
   * Construct an instance of {@link HttpHeaders} that contains everything needed to make a valid request to the
   * `/settlements` API endpoint on a Connector for a JSON payload and the supplied {@code idempotenceId}.
   *
   * @return An instance of {@link HttpHeaders}.
   */
  protected HttpHeaders testJsonHeaders(final String idempotenceId) {
    HttpHeaders headers = new HttpHeaders();
    if (idempotenceId != null) {
      headers.set(IDEMPOTENCY_KEY, idempotenceId);
    }
    headers.setAccept(Lists.newArrayList(MediaType.APPLICATION_JSON));
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

  /**
   * Construct an instance of {@link HttpHeaders} that contains everything needed to make a valid request to the
   * `/settlements` API endpoint on a Connector for a JSON payload with no idempotence header.
   *
   * @return An instance of {@link HttpHeaders}.
   */
  protected HttpHeaders testJsonHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Lists.newArrayList(MediaType.APPLICATION_JSON));
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

  /**
   * Construct an instance of {@link HttpHeaders} that contains everything needed to make a valid request to the
   * `/messages` API endpoint on a Connector for an Octet-Stream payload.
   *
   * @return An instance of {@link HttpHeaders}.
   */
  protected HttpHeaders testOctetStreamHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Lists.newArrayList(MediaType.APPLICATION_OCTET_STREAM));
    headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
    return headers;
  }
}
