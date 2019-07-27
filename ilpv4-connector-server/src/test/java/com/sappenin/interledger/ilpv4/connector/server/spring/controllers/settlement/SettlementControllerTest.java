package com.sappenin.interledger.ilpv4.connector.server.spring.controllers.settlement;

import com.google.common.collect.Lists;
import com.sappenin.interledger.ilpv4.connector.server.spring.controllers.AbstractControllerTest;
import com.sappenin.interledger.ilpv4.connector.server.spring.controllers.HeaderConstants;
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
import static com.sappenin.interledger.ilpv4.connector.server.spring.controllers.PathConstants.SLASH_SETTLEMENTS;
import static com.sappenin.interledger.ilpv4.connector.settlement.SettlementConstants.IDEMPOTENCY_KEY;
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

  static {
    System.setProperty("spring.cache.type", "redis");
  }

  @Autowired
  private MockMvc mvc;

  @Test
  public void sendSettlementMessageWithoutIdempotenceKey() throws Exception {
    SettlementQuantity settledSettlementQuantity = SettlementQuantity.builder()
      .amount(1L)
      .scale(6)
      .build();

    HttpHeaders headers = this.testHeaders();

    this.mvc
      .perform(post(SLASH_ACCOUNTS + SLASH + ALICE_ACCOUNT_ID.value() + SLASH_SETTLEMENTS)
        .headers(headers)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
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
  public void sendSettlementMessageWithNonUuidIdempotenceKey() throws Exception {
    SettlementQuantity settledSettlementQuantity = SettlementQuantity.builder()
      .amount(1L)
      .scale(6)
      .build();

    HttpHeaders headers = this.testHeaders("123");

    this.mvc
      .perform(post(SLASH_ACCOUNTS + SLASH + ALICE_ACCOUNT_ID.value() + SLASH_SETTLEMENTS)
        .headers(headers)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
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
  public void sendSettlementMessageTwice() throws Exception {
    final UUID idempotenceId = UUID.randomUUID();
    final SettlementQuantity settledSettlementQuantity = SettlementQuantity.builder()
      .amount(1L)
      .scale(6)
      .build();

    final HttpHeaders headers = this.testHeaders(idempotenceId.toString());
    final SettlementQuantity clearedSettlementQuantity = NumberScalingUtils.translate(settledSettlementQuantity, 9);
    when(settlementServiceMock.onLocalSettlementPayment(idempotenceId, ALICE_ACCOUNT_ID, settledSettlementQuantity))
      .thenReturn(clearedSettlementQuantity);

    // Make the call...
    this.mvc
      .perform(post(SLASH_ACCOUNTS + SLASH + ALICE_ACCOUNT_ID.value() + SLASH_SETTLEMENTS)
        .headers(headers)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(asJsonString(settledSettlementQuantity))
        .with(httpBasic("admin", "password")).with(csrf())
      )
      .andExpect(status().isOk())
      .andExpect(header().string(HeaderConstants.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE))
      .andExpect(header().string(IDEMPOTENCY_KEY, idempotenceId.toString()));

    // Call the endpoint a second time...
    this.mvc
      .perform(post(SLASH_ACCOUNTS + SLASH + ALICE_ACCOUNT_ID.value() + SLASH_SETTLEMENTS)
        .headers(headers)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(asJsonString(settledSettlementQuantity))
        .with(httpBasic("admin", "password")).with(csrf())
      )
      .andExpect(status().isOk())
      .andExpect(header().string(HeaderConstants.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE))
      .andExpect(header().string(IDEMPOTENCY_KEY, idempotenceId.toString()));

    verify(settlementServiceMock).onLocalSettlementPayment(idempotenceId, ALICE_ACCOUNT_ID, settledSettlementQuantity);

    // Due to request caching, the Controller should not be triggered more than once.
    verifyNoMoreInteractions(settlementServiceMock);
  }

  //////////////////
  // Private Helpers
  //////////////////

  /**
   * Construct an instance of {@link HttpHeaders} that contains everything needed to make a valid request to a
   * Settlement API endpoint on a Connector.
   *
   * @return An instance of {@link HttpHeaders}.
   */
  private HttpHeaders testHeaders(final String idempotenceId) {
    HttpHeaders headers = new HttpHeaders();
    if (idempotenceId != null) {
      headers.set(IDEMPOTENCY_KEY, idempotenceId);
    }
    headers.setAccept(Lists.newArrayList(MediaType.APPLICATION_JSON));
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

  private HttpHeaders testHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Lists.newArrayList(MediaType.APPLICATION_JSON));
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }
}
