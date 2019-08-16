package com.sappenin.interledger.ilpv4.connector.server.spring.controllers.settlement;

import com.sappenin.interledger.ilpv4.connector.server.spring.controllers.AbstractControllerTest;
import com.sappenin.interledger.ilpv4.connector.server.spring.controllers.HeaderConstants;
import org.interledger.connector.accounts.SettlementEngineAccountId;
import org.interledger.ilpv4.connector.core.settlement.SettlementQuantity;
import org.interledger.ilpv4.connector.settlement.NumberScalingUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigInteger;
import java.util.UUID;

import static com.sappenin.interledger.ilpv4.connector.server.spring.controllers.HeaderConstants.APPLICATION_PROBLEM_JSON;
import static com.sappenin.interledger.ilpv4.connector.server.spring.controllers.PathConstants.SLASH;
import static com.sappenin.interledger.ilpv4.connector.server.spring.controllers.PathConstants.SLASH_ACCOUNTS;
import static com.sappenin.interledger.ilpv4.connector.server.spring.controllers.PathConstants.SLASH_MESSAGES;
import static com.sappenin.interledger.ilpv4.connector.server.spring.controllers.PathConstants.SLASH_SETTLEMENTS;
import static org.interledger.ilpv4.connector.settlement.SettlementConstants.IDEMPOTENCY_KEY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ensures that the API endpoints for Settlements are valid.
 *
 * Note that this test does not spin-up a full runtime, but instead has a limited mock environment to isolate the {@link
 * SettlementController}.
 */
@RunWith(SpringRunner.class)
@WebMvcTest(controllers = SettlementController.class)
@SuppressWarnings("PMD")
public class SettlementControllerTest extends AbstractControllerTest {
  private static final byte[] MESSAGE = new byte[32];

  static {
    System.setProperty("spring.cache.type", "redis");
  }

  @Autowired
  private MockMvc mvc;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void sendSettlementWithoutIdempotenceKey() throws Exception {
    SettlementEngineAccountId settlementEngineAccountId =
      SettlementEngineAccountId.of(UUID.randomUUID().toString());

    SettlementQuantity settledSettlementQuantity = SettlementQuantity.builder()
      .amount(1L)
      .scale(6)
      .build();

    HttpHeaders headers = this.testJsonHeaders();

    this.mvc
      .perform(post(SLASH_ACCOUNTS + SLASH + settlementEngineAccountId.value() + SLASH_SETTLEMENTS)
        .headers(headers)
        .content(asJsonString(settledSettlementQuantity))
        .with(httpBasic("admin", "password")).with(csrf())
      )
      .andExpect(status().isBadRequest())
      .andExpect(header().string(HeaderConstants.CONTENT_TYPE, APPLICATION_PROBLEM_JSON))
      .andExpect(jsonPath("$.title").value("Bad Request"))
      .andExpect(jsonPath("$.status").value("400")) // TODO: Change once Problem support is fixed.
      .andExpect(jsonPath("$.detail")
        .value("Missing request header 'Idempotency-Key' for method parameter of type String")
      );
  }

  @Test
  public void sendSettlementWithNonUuidIdempotenceKey() throws Exception {
    SettlementEngineAccountId settlementEngineAccountId =
      SettlementEngineAccountId.of(UUID.randomUUID().toString());
    String idempotenceId = "123";
    SettlementQuantity settledSettlementQuantity = SettlementQuantity.builder()
      .amount(1L)
      .scale(6)
      .build();

    HttpHeaders headers = this.testJsonHeaders(idempotenceId);

    this.mvc
      .perform(post(SLASH_ACCOUNTS + SLASH + settlementEngineAccountId.value() + SLASH_SETTLEMENTS)
        .headers(headers)
        .content(asJsonString(settledSettlementQuantity))
        .with(httpBasic("admin", "password")).with(csrf())
      )
      .andExpect(status().isOk())
      .andExpect(header().string(HeaderConstants.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE))
      .andExpect(header().string(IDEMPOTENCY_KEY, idempotenceId));

    verify(settlementServiceMock)
      .onLocalSettlementPayment(idempotenceId, settlementEngineAccountId, settledSettlementQuantity);
  }

  @Test
  public void sendSettlementTwice() throws Exception {
    SettlementEngineAccountId settlementEngineAccountId =
      SettlementEngineAccountId.of(UUID.randomUUID().toString());

    final String idempotenceId = UUID.randomUUID().toString();
    final SettlementQuantity settledSettlementQuantity = SettlementQuantity.builder()
      .amount(1L)
      .scale(6)
      .build();

    final HttpHeaders headers = this.testJsonHeaders(idempotenceId.toString());
    final BigInteger clearedSettlementQuantity = NumberScalingUtils.translate(
      BigInteger.valueOf(settledSettlementQuantity.amount()),
      settledSettlementQuantity.scale(),
      9);
    when(settlementServiceMock
      .onLocalSettlementPayment(idempotenceId, settlementEngineAccountId, settledSettlementQuantity))
      .thenReturn(
        SettlementQuantity.builder()
          .amount(clearedSettlementQuantity.longValue())
          .scale(9)
          .build()
      );

    // Make the call...
    this.mvc
      .perform(post(SLASH_ACCOUNTS + SLASH + settlementEngineAccountId.value() + SLASH_SETTLEMENTS)
        .headers(headers)
        .content(asJsonString(settledSettlementQuantity))
        .with(httpBasic("admin", "password")).with(csrf())
      )
      .andExpect(status().isOk())
      .andExpect(header().string(HeaderConstants.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE))
      .andExpect(header().string(IDEMPOTENCY_KEY, idempotenceId));

    verify(settlementServiceMock)
      .onLocalSettlementPayment(idempotenceId, settlementEngineAccountId, settledSettlementQuantity);

    // Call the endpoint a second time...
    this.mvc
      .perform(post(SLASH_ACCOUNTS + SLASH + settlementEngineAccountId.value() + SLASH_SETTLEMENTS)
        .headers(headers)
        .content(asJsonString(settledSettlementQuantity))
        .with(httpBasic("admin", "password")).with(csrf())
      )
      .andExpect(status().isOk())
      .andExpect(header().string(HeaderConstants.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE))
      .andExpect(header().string(IDEMPOTENCY_KEY, idempotenceId));

    // Due to request caching, the Controller should not be triggered more than once.
    verifyNoMoreInteractions(settlementServiceMock);
  }

  @Test
  public void sendSettlementMessage() throws Exception {
    SettlementEngineAccountId settlementEngineAccountId =
      SettlementEngineAccountId.of(UUID.randomUUID().toString());
    when(settlementServiceMock.onLocalSettlementMessage(settlementEngineAccountId, MESSAGE)).thenReturn(MESSAGE);

    HttpHeaders headers = this.testOctetStreamHeaders();

    this.mvc
      .perform(post(SLASH_ACCOUNTS + SLASH + settlementEngineAccountId.value() + SLASH_MESSAGES)
        .headers(headers)
        .content(MESSAGE)
        .with(httpBasic("admin", "password")).with(csrf())
      )
      .andExpect(status().isOk())
      .andExpect(header().string(HeaderConstants.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE))
      .andExpect(header().doesNotExist(IDEMPOTENCY_KEY))
      .andExpect(content().bytes(MESSAGE));

    verify(settlementServiceMock).onLocalSettlementMessage(settlementEngineAccountId, MESSAGE);
  }

  @Test
  public void sendSettlementMessageTwice() throws Exception {
    SettlementEngineAccountId settlementEngineAccountId =
      SettlementEngineAccountId.of(UUID.randomUUID().toString());

    final HttpHeaders headers = this.testOctetStreamHeaders();
    when(settlementServiceMock.onLocalSettlementMessage(settlementEngineAccountId, MESSAGE)).thenReturn(MESSAGE);

    // Make the call...
    this.mvc
      .perform(post(SLASH_ACCOUNTS + SLASH + settlementEngineAccountId.value() + SLASH_MESSAGES)
        .headers(headers)
        .content(MESSAGE)
        .with(httpBasic("admin", "password")).with(csrf())
      )
      .andExpect(status().isOk())
      .andExpect(header().string(HeaderConstants.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE))
      .andExpect(header().doesNotExist(IDEMPOTENCY_KEY))
      .andExpect(content().bytes(MESSAGE));

    // Call the endpoint a second time...
    this.mvc
      .perform(post(SLASH_ACCOUNTS + SLASH + settlementEngineAccountId.value() + SLASH_MESSAGES)
        .headers(headers)
        .content(MESSAGE)
        .with(httpBasic("admin", "password")).with(csrf())
      )
      .andExpect(status().isOk())
      .andExpect(header().string(HeaderConstants.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE))
      .andExpect(header().doesNotExist(IDEMPOTENCY_KEY));

    verify(settlementServiceMock, Mockito.times(2)).onLocalSettlementMessage(eq(settlementEngineAccountId), any());
    verifyNoMoreInteractions(settlementServiceMock);
  }
}
