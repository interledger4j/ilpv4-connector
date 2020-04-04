package org.interledger.connector.opay.controllers;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.interledger.connector.opay.controllers.constants.PathConstants;
import org.interledger.connector.opay.model.OpenPaymentsMetadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.spring.common.MediaTypes;

import java.util.function.Supplier;

@RestController
public class OpenPaymentsMetadataController {

  @Autowired
  private ObjectMapper objectMapper;

  private Supplier<OpenPaymentsMetadata> openPaymentsMetadata;

  public OpenPaymentsMetadataController(Supplier<OpenPaymentsMetadata> openPaymentsMetadata) {
    this.openPaymentsMetadata = openPaymentsMetadata;
  }

  @RequestMapping(
    path = PathConstants.OPEN_PAYMENTS_METADATA,
    method = RequestMethod.GET,
    produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public @ResponseBody OpenPaymentsMetadata getOpenPaymentsMetadata() {
    return openPaymentsMetadata.get();
  }
}
