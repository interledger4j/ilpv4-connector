package org.interledger.connector.opay.controllers;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.interledger.connector.opay.InvoiceId;
import org.interledger.connector.opay.controllers.constants.PathConstants;
import org.interledger.connector.opay.model.Invoice;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.spring.common.MediaTypes;

@RestController
public class InvoicesController {

  @RequestMapping(
    path = PathConstants.SLASH_INVOICE,
    method = RequestMethod.POST,
    produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public @ResponseBody Invoice createInvoice() {
    return null;
  }

  @RequestMapping(
    path = PathConstants.SLASH_INVOICE + "/id",
    method = RequestMethod.GET,
    produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public @ResponseBody Invoice getInvoice(@PathVariable InvoiceId invoiceId) {
    return null;
  }

  @RequestMapping(
    path = PathConstants.SLASH_INVOICE + "/id",
    method = RequestMethod.OPTIONS,
    produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public @ResponseBody Invoice getPaymentDetails(@PathVariable InvoiceId invoiceId) {
    return null;
  }
}
