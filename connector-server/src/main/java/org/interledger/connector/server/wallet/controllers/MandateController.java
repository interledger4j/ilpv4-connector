package org.interledger.connector.server.wallet.controllers;

import static org.interledger.connector.core.ConfigConstants.ENABLED_PROTOCOLS;
import static org.interledger.connector.core.ConfigConstants.OPEN_PAYMENTS_ENABLED;
import static org.interledger.connector.core.ConfigConstants.TRUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.interledger.connector.accounts.AccountId;
import org.interledger.openpayments.Charge;
import org.interledger.openpayments.ChargeId;
import org.interledger.openpayments.Mandate;
import org.interledger.openpayments.MandateId;
import org.interledger.openpayments.NewCharge;
import org.interledger.openpayments.NewMandate;
import org.interledger.openpayments.PayIdAccountId;
import org.interledger.openpayments.config.OpenPaymentsPathConstants;
import org.interledger.openpayments.problems.ChargeNotFoundProblem;
import org.interledger.openpayments.problems.MandateNotFoundProblem;

import org.interleger.openpayments.mandates.MandateService;
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

import java.util.List;

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
   * @return A 201 Created if successful, and the fully populated {@link Mandate} which was stored.
   */
  @RequestMapping(
    path = OpenPaymentsPathConstants.MANDATES_BASE,
    method = RequestMethod.POST,
    consumes = {APPLICATION_JSON_VALUE},
    produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public @ResponseBody
  ResponseEntity<Mandate> createMandate(
    @PathVariable PayIdAccountId accountId,
    @RequestBody NewMandate newMandate
  ) {
    Mandate mandate = mandateService.createMandate(accountId, newMandate);
    final HttpHeaders headers = new HttpHeaders();
    headers.setLocation(mandate.id().uri());
    return new ResponseEntity(mandate, headers, HttpStatus.CREATED);
  }

  @RequestMapping(
    path = OpenPaymentsPathConstants.MANDATES_BASE,
    method = RequestMethod.GET,
    produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public @ResponseBody List<Mandate> listMandates(@PathVariable PayIdAccountId accountId) {
    return mandateService.findMandatesByAccountId(accountId);
  }

  @RequestMapping(
    path = OpenPaymentsPathConstants.MANDATES_WITH_ID,
    method = RequestMethod.GET,
    produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public @ResponseBody Mandate getMandate(@PathVariable PayIdAccountId accountId, @PathVariable MandateId mandateId) {
    return mandateService.findMandateById(accountId, mandateId).orElseThrow(() -> new MandateNotFoundProblem(mandateId));
  }

  /**
   * Create and return a charge on the Open Payments server.
   *
   * @param newCharge new charge request
   * @return A 201 Created if successful, and the fully populated {@link Charge} which was stored.
   */
  @RequestMapping(
    path = OpenPaymentsPathConstants.CHARGES_BASE,
    method = RequestMethod.POST,
    consumes = {APPLICATION_JSON_VALUE},
    produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public @ResponseBody
  ResponseEntity<Charge> createCharge(
    @PathVariable PayIdAccountId accountId,
    @PathVariable MandateId mandateId,
    @RequestBody NewCharge newCharge
  ) {
    Charge charge = mandateService.createCharge(accountId, mandateId, newCharge);
    final HttpHeaders headers = new HttpHeaders();
    headers.setLocation(charge.id().uri());
    return new ResponseEntity(charge, headers, HttpStatus.CREATED);
  }

  @RequestMapping(
    path = OpenPaymentsPathConstants.CHARGES_WITH_ID,
    method = RequestMethod.POST,
    produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public @ResponseBody Charge getCharge(
    @PathVariable PayIdAccountId accountId,
    @PathVariable MandateId mandateId,
    @PathVariable ChargeId chargeId) {
    return mandateService.findChargeById(accountId, mandateId, chargeId).orElseThrow(() -> new ChargeNotFoundProblem(chargeId));
  }


}
