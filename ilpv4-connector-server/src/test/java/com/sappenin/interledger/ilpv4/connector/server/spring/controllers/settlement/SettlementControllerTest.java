package com.sappenin.interledger.ilpv4.connector.server.spring.controllers.settlement;

import com.sappenin.interledger.ilpv4.connector.server.spring.controllers.AbstractControllerTest;
import com.sappenin.interledger.ilpv4.connector.server.spring.controllers.HeaderConstants;
import org.interledger.connector.accounts.SettlementEngineAccountId;
import org.interledger.ilpv4.connector.core.settlement.SettlementQuantity;
import org.interledger.ilpv4.connector.settlement.NumberScalingUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.sappenin.interledger.ilpv4.connector.server.spring.controllers.HeaderConstants.APPLICATION_PROBLEM_JSON;
import static com.sappenin.interledger.ilpv4.connector.server.spring.controllers.PathConstants.SLASH;
import static com.sappenin.interledger.ilpv4.connector.server.spring.controllers.PathConstants.SLASH_ACCOUNTS;
import static com.sappenin.interledger.ilpv4.connector.server.spring.controllers.PathConstants.SLASH_MESSAGES;
import static com.sappenin.interledger.ilpv4.connector.server.spring.controllers.PathConstants.SLASH_SETTLEMENTS;
import static org.interledger.ilpv4.connector.settlement.SettlementConstants.IDEMPOTENCY_KEY;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

  private static final SettlementEngineAccountId ALICE_SETTLEMENT_ACCOUNT_ID =
    SettlementEngineAccountId.of(UUID.randomUUID().toString());

  static {
    System.setProperty("spring.cache.type", "redis");
  }

  @Autowired
  private MockMvc mvc;

  @Test
  public void sendSettlementWithoutIdempotenceKey() throws Exception {
    SettlementQuantity settledSettlementQuantity = SettlementQuantity.builder()
      .amount(1L)
      .scale(6)
      .build();

    HttpHeaders headers = this.testJsonHeaders();

    this.mvc
      .perform(post(SLASH_ACCOUNTS + SLASH + ALICE_SETTLEMENT_ACCOUNT_ID.value() + SLASH_SETTLEMENTS)
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
    SettlementQuantity settledSettlementQuantity = SettlementQuantity.builder()
      .amount(1L)
      .scale(6)
      .build();

    HttpHeaders headers = this.testJsonHeaders("123");

    this.mvc
      .perform(post(SLASH_ACCOUNTS + SLASH + ALICE_SETTLEMENT_ACCOUNT_ID.value() + SLASH_SETTLEMENTS)
        .headers(headers)
        .content(asJsonString(settledSettlementQuantity))
        .with(httpBasic("admin", "password")).with(csrf())
      )
      .andExpect(status().isBadRequest())
      .andExpect(header().string(HeaderConstants.CONTENT_TYPE, APPLICATION_PROBLEM_JSON))
      .andExpect(jsonPath("$.title").value("Invalid Idempotency Key"))
      .andExpect(jsonPath("$.status").value("400")) // TODO: Change once Problem support is fixed.
      .andExpect(jsonPath("$.detail").value("The `Idempotency-Key` header must be a Type4 UUID"));
  }

  @Test
  public void sendSettlementTwice() throws Exception {
    final String idempotenceId = UUID.randomUUID().toString();
    final SettlementQuantity settledSettlementQuantity = SettlementQuantity.builder()
      .amount(1L)
      .scale(6)
      .build();

    final HttpHeaders headers = this.testJsonHeaders(idempotenceId.toString());
    final SettlementQuantity clearedSettlementQuantity = NumberScalingUtils.translate(settledSettlementQuantity, 9);
    when(settlementServiceMock
      .onLocalSettlementPayment(idempotenceId, ALICE_SETTLEMENT_ACCOUNT_ID, settledSettlementQuantity))
      .thenReturn(clearedSettlementQuantity);

    // Make the call...
    this.mvc
      .perform(post(SLASH_ACCOUNTS + SLASH + ALICE_SETTLEMENT_ACCOUNT_ID.value() + SLASH_SETTLEMENTS)
        .headers(headers)
        .content(asJsonString(settledSettlementQuantity))
        .with(httpBasic("admin", "password")).with(csrf())
      )
      .andExpect(status().isOk())
      .andExpect(header().string(HeaderConstants.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE))
      .andExpect(header().string(IDEMPOTENCY_KEY, idempotenceId.toString()));

    verify(settlementServiceMock)
      .onLocalSettlementPayment(idempotenceId, ALICE_SETTLEMENT_ACCOUNT_ID, settledSettlementQuantity);

    // Call the endpoint a second time...
    this.mvc
      .perform(post(SLASH_ACCOUNTS + SLASH + ALICE_SETTLEMENT_ACCOUNT_ID.value() + SLASH_SETTLEMENTS)
        .headers(headers)
        .content(asJsonString(settledSettlementQuantity))
        .with(httpBasic("admin", "password")).with(csrf())
      )
      .andExpect(status().isOk())
      .andExpect(header().string(HeaderConstants.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE))
      .andExpect(header().string(IDEMPOTENCY_KEY, idempotenceId.toString()));

    // Due to request caching, the Controller should not be triggered more than once.
    verifyNoMoreInteractions(settlementServiceMock);
  }

  @Test
  public void sendSettlementMessageWithoutIdempotenceKey() throws Exception {
    byte[] message = new byte[32];
    HttpHeaders headers = this.testOctetStreamHeaders();

    this.mvc
      .perform(post(SLASH_ACCOUNTS + SLASH + ALICE_SETTLEMENT_ACCOUNT_ID.value() + SLASH_MESSAGES)
        .headers(headers)
        .content(message)
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
  public void sendSettlementMessageTwice() throws Exception {
    byte[] message = new byte[32];
    final HttpHeaders headers = this.testOctetStreamHeaders();

    when(settlementServiceMock.onLocalSettlementMessage(ALICE_SETTLEMENT_ACCOUNT_ID, message))
      .thenReturn(message);

    // Make the call...
    this.mvc
      .perform(post(SLASH_ACCOUNTS + SLASH + ALICE_SETTLEMENT_ACCOUNT_ID.value() + SLASH_MESSAGES)
        .headers(headers)
        .content(message)
        .with(httpBasic("admin", "password")).with(csrf())
      )
      .andExpect(status().isOk())
      .andExpect(header().string(HeaderConstants.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE))
      .andExpect(header().doesNotExist(IDEMPOTENCY_KEY));

    verify(settlementServiceMock).onLocalSettlementMessage(ALICE_SETTLEMENT_ACCOUNT_ID, message);

    // Call the endpoint a second time...
    this.mvc
      .perform(post(SLASH_ACCOUNTS + SLASH + ALICE_SETTLEMENT_ACCOUNT_ID.value() + SLASH_MESSAGES)
        .headers(headers)
        .content(message)
        .with(httpBasic("admin", "password")).with(csrf())
      )
      .andExpect(status().isOk())
      .andExpect(header().string(HeaderConstants.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE))
      .andExpect(header().doesNotExist(IDEMPOTENCY_KEY));

    // Due to request caching, the Controller should not be triggered more than once.
    verifyNoMoreInteractions(settlementServiceMock);
  }

}
