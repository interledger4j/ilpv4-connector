package org.interledger.connector.server.wallet.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.interledger.connector.opa.model.PayIdOpaPaymentRequest;
import org.interledger.connector.opa.model.PaymentResponse;
import org.interledger.connector.server.spring.controllers.AbstractControllerTest;
import org.interledger.connector.settings.properties.OpenPaymentsPathConstants;

import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@RunWith(SpringRunner.class)
@WebMvcTest(
  controllers = OpaPaymentsController.class
)
public class OpenPaymentsControllerTest extends AbstractControllerTest {

  @Autowired
  MockMvc mockMvc;

  @Before
  public void setUp() {
    initMocks(this);
  }

  @Test
  public void sendOpaPayment() throws Exception {
    PayIdOpaPaymentRequest payIdOpaPaymentRequest = PayIdOpaPaymentRequest.builder()
      .amount(UnsignedLong.valueOf(1000))
      .destinationPaymentPointer("$example.com/foo")
      .build();

    PaymentResponse paymentResponse = PaymentResponse.builder()
      .originalAmount(UnsignedLong.valueOf(1000))
      .amountSent(UnsignedLong.valueOf(1000))
      .amountDelivered(UnsignedLong.valueOf(1000))
      .successfulPayment(true)
      .build();

    when(ilpOpenPaymentService.sendOpaPayment(eq(payIdOpaPaymentRequest), eq("foo"), any()))
    .thenReturn(paymentResponse);
    mockMvc.perform(post(OpenPaymentsPathConstants.SLASH_ACCOUNTS + "/foo" + OpenPaymentsPathConstants.SLASH_OPA + OpenPaymentsPathConstants.SLASH_ILP)
      .headers(this.testJsonHeaders())
      .content(objectMapper.writeValueAsString(payIdOpaPaymentRequest))
      .with(httpBasic("admin", "password")).with(csrf())
    )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.originalAmount").value(paymentResponse.originalAmount().longValue()))
      .andExpect(jsonPath("$.amountSent").value(paymentResponse.amountSent().longValue()))
      .andExpect(jsonPath("$.amountDelivered").value(paymentResponse.amountDelivered().longValue()))
      .andExpect(jsonPath("$.successfulPayment").value(paymentResponse.successfulPayment()));
  }
}
