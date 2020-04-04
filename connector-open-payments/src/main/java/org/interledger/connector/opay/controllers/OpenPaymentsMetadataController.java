package org.interledger.connector.opay.controllers;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.interledger.connector.opay.controllers.constants.PathConstants;
import org.interledger.connector.opay.model.OpenPaymentsMetadata;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.spring.common.MediaTypes;

@RestController
public class OpenPaymentsMetadataController {

  private OpenPaymentsMetadata openPaymentsMetadata;

  public OpenPaymentsMetadataController(OpenPaymentsMetadata openPaymentsMetadata) {
    this.openPaymentsMetadata = openPaymentsMetadata;
  }

  @RequestMapping(
    path = PathConstants.OPEN_PAYMENTS_METADATA,
    method = RequestMethod.GET,
    produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public OpenPaymentsMetadata getOpenPaymentsMetadata() {
    return openPaymentsMetadata;
  }
}
