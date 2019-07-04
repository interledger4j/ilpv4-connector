package com.sappenin.interledger.ilpv4.connector.server.spring.controllers.settlement;

import com.google.common.collect.Lists;
import com.sappenin.interledger.ilpv4.connector.server.spring.controllers.HeaderConstants;
import com.sappenin.interledger.ilpv4.connector.settlement.IdempotentResponseInfo;
import org.interledger.ilpv4.connector.core.settlement.Quantity;
import org.interledger.ilpv4.connector.settlement.NumberScalingUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigInteger;
import java.util.Optional;
import java.util.UUID;

import static com.sappenin.interledger.ilpv4.connector.server.spring.controllers.HeaderConstants.APPLICATION_PROBLEM_JSON;
import static com.sappenin.interledger.ilpv4.connector.server.spring.controllers.HeaderConstants.IDEMPOTENCY_KEY;
import static com.sappenin.interledger.ilpv4.connector.server.spring.controllers.PathConstants.SLASH;
import static com.sappenin.interledger.ilpv4.connector.server.spring.controllers.PathConstants.SLASH_ACCOUNTS;
import static com.sappenin.interledger.ilpv4.connector.server.spring.controllers.PathConstants.SLASH_SETTLEMENTS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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
public class SettlementControllerTest extends AbstractControllerTest {

  @Autowired
  private MockMvc mvc;

  @Before
  public void setup() {
  }

  @Test
  public void sendSettlementMessageWithoutIdempotenceKey() throws Exception {
    Quantity settledQuantity = Quantity.builder()
      .amount(BigInteger.valueOf(1))
      .scale(6)
      .build();

    HttpHeaders headers = this.testHeaders();

    this.mvc
      .perform(post(SLASH_ACCOUNTS + SLASH + ALICE_ACCOUNT_ID.value() + SLASH_SETTLEMENTS)
        .headers(headers)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(asJsonString(settledQuantity))
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
    Quantity settledQuantity = Quantity.builder()
      .amount(BigInteger.valueOf(1))
      .scale(6)
      .build();

    HttpHeaders headers = this.testHeaders("123");


    this.mvc
      .perform(post(SLASH_ACCOUNTS + SLASH + ALICE_ACCOUNT_ID.value() + SLASH_SETTLEMENTS)
        .headers(headers)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(asJsonString(settledQuantity))
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
    UUID idempotenceId = UUID.randomUUID();

    Quantity settledQuantity = Quantity.builder()
      .amount(BigInteger.valueOf(1))
      .scale(6)
      .build();
    Quantity clearedQuantity = NumberScalingUtils.translate(settledQuantity, 9);

    HttpHeaders headers = this.testHeaders(idempotenceId.toString());
    when(idempotenceServiceMock.reserveRequestId(idempotenceId)).thenReturn(true);
    when(settlementServiceMock.handleIncomingSettlement(idempotenceId, ALICE_ACCOUNT_ID, settledQuantity))
      .thenReturn(clearedQuantity);
    when(idempotenceServiceMock.updateIdempotenceRecord(any())).thenReturn(true);

    this.mvc
      .perform(post(SLASH_ACCOUNTS + SLASH + ALICE_ACCOUNT_ID.value() + SLASH_SETTLEMENTS)
        .headers(headers)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(asJsonString(settledQuantity))
        .with(httpBasic("admin", "password")).with(csrf())
      )
      .andExpect(status().isOk())
      .andExpect(header().string(HeaderConstants.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE))
      .andExpect(header().string(IDEMPOTENCY_KEY, idempotenceId.toString()));

    HttpHeaders cachedHeaders = new HttpHeaders();
    cachedHeaders.set(IDEMPOTENCY_KEY, idempotenceId.toString());
    final IdempotentResponseInfo cachedResponse =
      IdempotentResponseInfo.builder()
        .responseStatus(HttpStatus.OK)
        .requestId(idempotenceId)
        .responseBody(clearedQuantity)
        .responseHeaders(cachedHeaders)
        .build();

    when(idempotenceServiceMock.getIdempotenceRecord(idempotenceId)).thenReturn(Optional.of(cachedResponse));

    // Call the endpoint twice
    this.mvc
      .perform(post(SLASH_ACCOUNTS + SLASH + ALICE_ACCOUNT_ID.value() + SLASH_SETTLEMENTS)
        .headers(headers)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(asJsonString(settledQuantity))
        .with(httpBasic("admin", "password")).with(csrf())
      )
      .andExpect(status().isOk())
      .andExpect(header().string(HeaderConstants.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE))
      .andExpect(header().string(IDEMPOTENCY_KEY, idempotenceId.toString()));

    verify(settlementServiceMock).handleIncomingSettlement(idempotenceId, ALICE_ACCOUNT_ID, settledQuantity);
    verify(idempotenceServiceMock).updateIdempotenceRecord(any());

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
      headers.set(HeaderConstants.IDEMPOTENCY_KEY, idempotenceId);
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
