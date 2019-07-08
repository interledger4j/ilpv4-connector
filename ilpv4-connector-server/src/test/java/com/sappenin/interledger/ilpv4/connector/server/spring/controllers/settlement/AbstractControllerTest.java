package com.sappenin.interledger.ilpv4.connector.server.spring.controllers.settlement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sappenin.interledger.ilpv4.connector.links.LinkSettingsFactory;
import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettings;
import com.sappenin.interledger.ilpv4.connector.settlement.IdempotentRequestCache;
import com.sappenin.interledger.ilpv4.connector.settlement.SettlementService;
import org.interledger.connector.accounts.AccountId;
import org.interledger.crypto.EncryptionService;
import org.interledger.ilpv4.connector.persistence.repositories.AccountSettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.util.function.Supplier;

/**
 * An abstract super class for all Controller tests.
 */
@ContextConfiguration(classes = {ControllerConfig.class})
@ActiveProfiles({"test"}) // Uses the `application-test.properties` file in the `src/test/resources` folder
public abstract class AbstractControllerTest {

  protected static final AccountId ALICE_ACCOUNT_ID = AccountId.of("alice");

  @Autowired
  protected ObjectMapper objectMapper;

  @MockBean
  protected IdempotentRequestCache idempotentRequestCacheMock;

  @MockBean
  protected SettlementService settlementServiceMock;

  @MockBean
  protected AccountSettingsRepository accountSettingsRepositoryMock;

  @MockBean
  protected ConnectorSettings connectorSettingsMock;

  @MockBean
  protected Supplier<ConnectorSettings> connectorSettingsSupplierMock;

  @MockBean
  protected EncryptionService encryptionServiceMock;

  @MockBean
  protected LinkSettingsFactory linkSettingsFactory;

  protected String asJsonString(final Object obj) throws JsonProcessingException {
      return this.objectMapper.writeValueAsString(obj);
  }
}
