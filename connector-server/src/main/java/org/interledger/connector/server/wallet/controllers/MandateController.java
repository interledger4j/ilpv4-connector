package org.interledger.connector.server.wallet.controllers;

import static org.interledger.connector.core.ConfigConstants.ENABLED_PROTOCOLS;
import static org.interledger.connector.core.ConfigConstants.OPEN_PAYMENTS_ENABLED;
import static org.interledger.connector.core.ConfigConstants.TRUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.Mandate;
import org.interledger.connector.opa.model.NewMandate;
import org.interledger.connector.settings.properties.OpenPaymentsPathConstants;
import org.interledger.connector.wallet.mandates.MandateService;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.spring.common.MediaTypes;

@RestController
@ConditionalOnProperty(prefix = ENABLED_PROTOCOLS, name = OPEN_PAYMENTS_ENABLED, havingValue = TRUE)
public class MandateController {

  private final MandateService mandateService;

  public MandateController(MandateService mandateService) {
    this.mandateService = mandateService;
  }

  /**
   * Create and return an mandate on the Open Payments server.
   *
   * @param newMandate new mandate request
   * @return A 201 Created if successful, and the fully populated {@link Invoice} which was stored.
   */
  @RequestMapping(
    path = OpenPaymentsPathConstants.MANDATES_BASE,
    method = RequestMethod.POST,
    produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public @ResponseBody
  ResponseEntity<Mandate> createInvoice(
    @PathVariable AccountId accountId,
    @RequestBody NewMandate newMandate
  ) {
    Mandate mandate = mandateService.createMandate(accountId, newMandate);
    final HttpHeaders headers = new HttpHeaders();
    headers.setLocation(mandate.id().uri());
    return new ResponseEntity(mandate, headers, HttpStatus.CREATED);
  }

}
