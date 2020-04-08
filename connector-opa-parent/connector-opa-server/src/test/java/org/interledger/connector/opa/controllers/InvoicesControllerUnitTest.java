package org.interledger.connector.opa.controllers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.interledger.connector.opa.controllers.constants.PathConstants;
import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.InvoiceId;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.SharedSecret;
import org.interledger.spsp.StreamConnectionDetails;
import org.interledger.stream.crypto.Random;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@RunWith(SpringRunner.class)
@WebMvcTest(
  controllers = InvoicesController.class,
  excludeAutoConfiguration = {SecurityAutoConfiguration.class}
)
public class InvoicesControllerUnitTest extends AbstractControllerTest {

  @Autowired
  MockMvc mockMvc;

  @Before
  public void setUp() {
    initMocks(this);
  }

  @Test
  public void getPaymentDetailsForInvoice() throws Exception {
    InvoiceId invoiceId = InvoiceId.of("66ce60d8-f4ba-4c60-ba6e-fc5e0aa99923");
    String encodedInvoiceId = "NjZjZTYwZDgtZjRiYS00YzYwLWJhNmUtZmM1ZTBhYTk5OTIz";

    Invoice mockInvoice = mock(Invoice.class);
    when(invoiceServiceMock.getInvoiceById(invoiceId)).thenReturn(mockInvoice);
    when(mockInvoice.subject()).thenReturn("$foo.bar/baz");
    when(invoiceServiceMock.getAddressFromInvoiceSubject(mockInvoice.subject())).thenReturn("test.foo.bar.123456");

    InterledgerAddress destinationAddress = InterledgerAddress.of("test.foo.bar.123456");
    StreamConnectionDetails streamConnectionDetails = StreamConnectionDetails.builder()
      .destinationAddress(destinationAddress)
      .sharedSecret(SharedSecret.of(Random.randBytes(32)))
      .build();

    when(streamConnectionGeneratorMock.generateConnectionDetails(serverSecretSupplierMock, destinationAddress))
      .thenReturn(streamConnectionDetails);

    mockMvc
      .perform(options(PathConstants.SLASH_INVOICE + PathConstants.SLASH + invoiceId)
        .headers(this.testJsonHeaders())
      )
      .andExpect(status().isOk())
    .andExpect(jsonPath("$.destination_account").value(destinationAddress.with("~" + encodedInvoiceId).getValue()));
  }

}
