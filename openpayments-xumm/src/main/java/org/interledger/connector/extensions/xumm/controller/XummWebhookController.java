package org.interledger.connector.extensions.xumm.controller;

import org.interledger.connector.xumm.model.callback.PayloadCallback;
import org.interledger.connector.xumm.service.XummPaymentService;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnBean(XummPaymentService.class)
public class XummWebhookController {

  private final XummPaymentService xummPaymentService;

  public XummWebhookController(XummPaymentService xummPaymentService) {
    this.xummPaymentService = xummPaymentService;
  }

  @RequestMapping(
    path = "/webhooks/xumm",
    method = RequestMethod.POST
  )
  public ResponseEntity webhook(@RequestBody PayloadCallback payloadCallback) {
    xummPaymentService.handle(payloadCallback);
    return ResponseEntity.ok().build();
  }


}
