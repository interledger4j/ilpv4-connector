package org.interledger.connector.server.spring.controllers;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.interledger.connector.server.spring.controllers.model.ConnectorSettingsResponse;
import org.interledger.connector.settings.ConnectorSettings;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.spring.common.MediaTypes;

import java.util.function.Supplier;

/**
 * A RESTful controller for returning meta-data about the currently running Connector runtime.
 *
 */
@RestController
public class MetaDataController {

  private Supplier<ConnectorSettings> connectorSettingsSupplier;

  @Autowired
  private BuildProperties buildProperties;

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
      .version(this.buildProperties.getVersion())
      .build(), HttpStatus.OK);
  }
}
