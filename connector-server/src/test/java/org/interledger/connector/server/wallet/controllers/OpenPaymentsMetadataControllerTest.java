package org.interledger.connector.server.wallet.controllers;

import static org.mockito.MockitoAnnotations.initMocks;

import org.interledger.connector.server.openpayments.controllers.OpenPaymentsMetadataController;
import org.interledger.connector.server.spring.controllers.AbstractControllerTest;
import org.interledger.openpayments.config.OpenPaymentsSettings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.function.Supplier;

@RunWith(SpringRunner.class)
@WebMvcTest(
  properties = {"interledger.connector.enabledProtocols.openPayments=true"},
  controllers = OpenPaymentsMetadataController.class,
  excludeAutoConfiguration = {SecurityAutoConfiguration.class,}
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
    // FIXME: test something
  }
}
