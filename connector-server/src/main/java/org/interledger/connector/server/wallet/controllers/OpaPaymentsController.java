package org.interledger.connector.server.wallet.controllers;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.interledger.connector.opa.OpaPaymentService;
import org.interledger.connector.opa.model.PaymentRequest;
import org.interledger.connector.opa.model.PaymentResponse;
import org.interledger.connector.settings.properties.OpenPaymentsPathConstants;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.spring.common.MediaTypes;

import java.util.concurrent.ExecutionException;

@RestController
public class OpaPaymentsController {

  private static final String APPLICATION_JSON_XRP_OPA_VALUE = "application/json+xrp-opa";
  private static final MediaType APPLICATION_JSON_XRP_OPA = MediaType.valueOf(APPLICATION_JSON_XRP_OPA_VALUE);

  private OpaPaymentService ilpOpaPaymentService;

  public OpaPaymentsController(OpaPaymentService ilpOpaPaymentService) {
    this.ilpOpaPaymentService = ilpOpaPaymentService;
  }

  @RequestMapping(
    path = OpenPaymentsPathConstants.SLASH_ACCOUNTS_OPA_ILP,
    method = RequestMethod.POST,
    produces = {APPLICATION_JSON_XRP_OPA_VALUE, APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public @ResponseBody ResponseEntity<PaymentResponse> sendOpaPayment(
    @RequestHeader("Authorization") String bearerToken,
    @RequestBody PaymentRequest paymentRequest,
    @RequestHeader("Content-Type") String contentTypeHeader,
    @PathVariable(OpenPaymentsPathConstants.ACCOUNT_ID) String accountId
  ) throws ExecutionException, InterruptedException {
    // We want this endpoint to service sending OPA payments over ILP and XRP. If the Content-Type
    // header in the request is application/json+xrp-opa, then we should send an XRP payment under OPA.
    if (contentTypeHeader.equals(APPLICATION_JSON_XRP_OPA_VALUE)) {
      return new ResponseEntity(HttpStatus.BAD_REQUEST);
    }

    PaymentResponse paymentResponse = ilpOpaPaymentService.sendOpaPayment(paymentRequest, accountId, bearerToken);
    return new ResponseEntity(paymentResponse, HttpStatus.OK);
  }
}
