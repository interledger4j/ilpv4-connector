package org.interledger.connector.server.wallet.controllers;

import static org.mockito.MockitoAnnotations.initMocks;

import org.interledger.connector.server.spring.controllers.AbstractControllerTest;
import org.interledger.openpayments.config.OpenPaymentsSettings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.function.Supplier;

@RunWith(SpringRunner.class)
@WebMvcTest(
  controllers = InvoicesController.class
)
public class InvoicesControllerTest extends AbstractControllerTest {

  @Autowired
  MockMvc mockMvc;

  @Autowired
  Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier;

  @Before
  public void setUp() {
    initMocks(this);
  }

  @Test
  public void crud() {
    // FIXME test something
  }

}
