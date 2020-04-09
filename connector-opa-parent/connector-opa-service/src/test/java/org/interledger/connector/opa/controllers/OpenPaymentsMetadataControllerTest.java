package org.interledger.connector.opa.controllers;

import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.interledger.connector.opa.config.OpenPaymentsSettings;
import org.interledger.connector.opa.controllers.constants.PathConstants;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.function.Supplier;

@RunWith(SpringRunner.class)
@WebMvcTest(
  controllers = OpenPaymentsMetadataController.class,
  excludeAutoConfiguration = {SecurityAutoConfiguration.class}
)
public class OpenPaymentsMetadataControllerTest extends AbstractControllerTest {
  @Autowired
  private MockMvc mvc;

  @Autowired
  private Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier;

  @Before
  public void setUp() {
    initMocks(this);
  }

  @Test
  public void getOpenPaymentsMetadata() throws Exception {
    HttpHeaders headers = this.testJsonHeaders();

    this.mvc
      .perform(get(PathConstants.OPEN_PAYMENTS_METADATA)
        .headers(headers)
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.issuer").value(openPaymentsSettingsSupplier.get().metadata().issuer().toString()))
      .andExpect(jsonPath("$.authorization_issuer").value(openPaymentsSettingsSupplier.get().metadata().authorizationIssuer().toString()))
      .andExpect(jsonPath("$.authorization_endpoint").value(openPaymentsSettingsSupplier.get().metadata().authorizationEndpoint().toString()))
      .andExpect(jsonPath("$.token_endpoint").value(openPaymentsSettingsSupplier.get().metadata().tokenEndpoint().toString()))
      .andExpect(jsonPath("$.invoices_endpoint").value(openPaymentsSettingsSupplier.get().metadata().invoicesEndpoint().toString()))
      .andExpect(jsonPath("$.mandates_endpoint").value(openPaymentsSettingsSupplier.get().metadata().mandatesEndpoint().toString()))
      .andExpect(jsonPath("$.assets_supported[0].code").value(openPaymentsSettingsSupplier.get().metadata().assetsSupported().get(0).assetCode()))
      .andExpect(jsonPath("$.assets_supported[0].scale").value((int) openPaymentsSettingsSupplier.get().metadata().assetsSupported().get(0).assetScale()))
      .andExpect(jsonPath("$.assets_supported[1].code").value(openPaymentsSettingsSupplier.get().metadata().assetsSupported().get(1).assetCode()))
      .andExpect(jsonPath("$.assets_supported[1].scale").value((int) openPaymentsSettingsSupplier.get().metadata().assetsSupported().get(1).assetScale()));
  }
}
