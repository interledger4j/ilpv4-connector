package org.interledger.connector.xumm.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.interledger.connector.xumm.model.callback.PayloadCallback;
import org.interledger.connector.xumm.service.XummPaymentService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class XummWebhookController {

  private final XummPaymentService xummPaymentService;

  public XummWebhookController(XummPaymentService xummPaymentService) {
    this.xummPaymentService = xummPaymentService;
  }

  @RequestMapping(
    path = "/xumm/webhook",
    method = RequestMethod.POST,
    consumes = {APPLICATION_JSON_VALUE}
  )
  public ResponseEntity webhook(PayloadCallback payloadCallback) {
    xummPaymentService.handle(payloadCallback);
    return ResponseEntity.ok().build();
  }


}
