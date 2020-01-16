package org.interledger.connector.server.spring.controllers;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.interledger.connector.server.spring.controllers.model.ConnectorSettingsResponse;
import org.interledger.connector.settings.ConnectorSettings;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.spring.common.MediaTypes;

import java.util.function.Supplier;

/**
 * A RESTful controller for handling ILP over HTTP request/response payloads.
 *
 * @see "https://github.com/interledger/rfcs/blob/master/0035-ilp-over-http/0035-ilp-over-http.md"
 */
@RestController
public class MetaDataController {

  Supplier<ConnectorSettings> connectorSettingsSupplier;

  /**
   * Autoconfigured from maven build.  Has to not be required and checked for null,
   * otherwise WebMvcTests in our suite will fail when they can't autowire this bean
   */
  @Autowired(required = false)
  BuildProperties buildProperties;

  public MetaDataController(Supplier<ConnectorSettings> connectorSettingsSupplier) {
    this.connectorSettingsSupplier = connectorSettingsSupplier;
  }

  @RequestMapping(
    path = PathConstants.SLASH,
    method = RequestMethod.GET,
    produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public ResponseEntity<ConnectorSettingsResponse> getConnectorMetaData() {
    return new ResponseEntity<>(ConnectorSettingsResponse.builder()
      .operatorAddress(this.connectorSettingsSupplier.get().operatorAddress())
      .version(this.buildProperties == null ? "unknown" : this.buildProperties.getVersion())
      .build(), HttpStatus.OK);
  }
}
