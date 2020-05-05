package org.interledger.connector.server.wallet.controllers;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.interledger.connector.opa.model.OpenPaymentsMetadata;
import org.interledger.connector.opa.model.OpenPaymentsSettings;
import org.interledger.connector.server.spring.controllers.PathConstants;
import org.interledger.connector.settings.properties.OpenPaymentsPathConstants;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.spring.common.MediaTypes;

import java.util.function.Supplier;

/**
 * Controller to serve the Open Payments discovery resource.
 */
@RestController
public class OpenPaymentsMetadataController {

  private Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier;

  public OpenPaymentsMetadataController(Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier) {
    this.openPaymentsSettingsSupplier = openPaymentsSettingsSupplier;
  }

  /**
   * Get the metadata for the Open Payments server.
   *
   * @return The {@link OpenPaymentsMetadata} for this server.
   */
  @RequestMapping(
    path = OpenPaymentsPathConstants.SLASH_ACCOUNT_ID,
    method = RequestMethod.GET,
    produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public @ResponseBody OpenPaymentsMetadata getOpenPaymentsMetadata() {
    return openPaymentsSettingsSupplier.get().metadata();
  }
}
