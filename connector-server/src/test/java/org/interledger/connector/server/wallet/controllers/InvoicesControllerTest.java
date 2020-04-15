package org.interledger.connector.server.wallet.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.InvoiceId;
import org.interledger.connector.opa.model.problems.InvalidInvoiceSubjectProblem;
import org.interledger.connector.opa.model.problems.InvoiceNotFoundProblem;
import org.interledger.connector.server.spring.controllers.AbstractControllerTest;
import org.interledger.connector.server.spring.controllers.PathConstants;
import org.interledger.connector.settings.properties.OpenPaymentsPathConstants;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.SharedSecret;
import org.interledger.spsp.StreamConnectionDetails;
import org.interledger.stream.crypto.Random;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;

@RunWith(SpringRunner.class)
@WebMvcTest(
  controllers = InvoicesController.class
)
public class InvoicesControllerTest extends AbstractControllerTest {

  @Autowired
  MockMvc mockMvc;

  @Before
  public void setUp() {
    initMocks(this);
  }

  @Test
  public void getExistingInvoice() throws Exception {
    Invoice invoiceMock = Invoice.builder()
      .accountId("foo")
      .amount(UnsignedLong.valueOf(1000))
      .assetCode("XRP")
      .assetScale((short) 9)
      .subject("$wallet.com/foo")
      .expiresAt(Instant.MAX)
      .received(UnsignedLong.valueOf(1000))
      .description("Test invoice")
      .build();

    when(invoiceServiceMock.getInvoiceById(invoiceMock.id())).thenReturn(invoiceMock);

    mockMvc
      .perform(get(OpenPaymentsPathConstants.SLASH_INVOICE + PathConstants.SLASH + invoiceMock.id())
        .headers(this.testJsonHeaders()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.accountId").value(invoiceMock.accountId()))
      .andExpect(jsonPath("$.amount").value(invoiceMock.amount().longValue()))
      .andExpect(jsonPath("$.assetCode").value(invoiceMock.assetCode()))
      .andExpect(jsonPath("$.assetScale").value((int) invoiceMock.assetScale()))
      .andExpect(jsonPath("$.subject").value(invoiceMock.subject()))
      .andExpect(jsonPath("$.expiresAt").value(invoiceMock.expiresAt().toString()))
      .andExpect(jsonPath("$.received").value(invoiceMock.received().longValue()))
      .andExpect(jsonPath("$.description").value(invoiceMock.description()));
  }

  @Test
  public void getNonExistentInvoice() throws Exception {
    Invoice invoiceMock = Invoice.builder()
      .accountId("foo")
      .amount(UnsignedLong.valueOf(1000))
      .assetCode("XRP")
      .assetScale((short) 9)
      .subject("$wallet.com/foo")
      .expiresAt(Instant.MAX)
      .received(UnsignedLong.valueOf(1000))
      .description("Test invoice")
      .build();

    when(invoiceServiceMock.getInvoiceById(invoiceMock.id())).thenThrow(new InvoiceNotFoundProblem(invoiceMock.id()));
    mockMvc
      .perform(get(OpenPaymentsPathConstants.SLASH_INVOICE + PathConstants.SLASH + invoiceMock.id())
        .headers(this.testJsonHeaders()))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.type").value("https://errors.interledger.org/invoices/invoice-not-found"));
  }

  @Test
  public void createValidInvoice() throws Exception {
    Invoice invoiceMock = Invoice.builder()
      .accountId("foo")
      .amount(UnsignedLong.valueOf(1000))
      .assetCode("XRP")
      .assetScale((short) 9)
      .subject("$wallet.com/foo")
      .expiresAt(Instant.MAX)
      .received(UnsignedLong.valueOf(1000))
      .description("Test invoice")
      .build();

    when(invoiceServiceMock.createInvoice(any())).thenReturn(invoiceMock);

    mockMvc
      .perform(post(OpenPaymentsPathConstants.SLASH_INVOICE)
        .headers(this.testJsonHeaders())
        .content(objectMapper.writeValueAsString(invoiceMock))
      )
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.accountId").value(invoiceMock.accountId()))
      .andExpect(jsonPath("$.amount").value(invoiceMock.amount().longValue()))
      .andExpect(jsonPath("$.assetCode").value(invoiceMock.assetCode()))
      .andExpect(jsonPath("$.assetScale").value((int) invoiceMock.assetScale()))
      .andExpect(jsonPath("$.subject").value(invoiceMock.subject()))
      .andExpect(jsonPath("$.expiresAt").value(invoiceMock.expiresAt().toString()))
      .andExpect(jsonPath("$.received").value(invoiceMock.received().longValue()))
      .andExpect(jsonPath("$.description").value(invoiceMock.description()));
  }

  @Test
  public void createInvoiceInvalidSubjectPaymentPointer() throws Exception {
    Invoice invoiceMock = Invoice.builder()
      .accountId("foo")
      .amount(UnsignedLong.valueOf(1000))
      .assetCode("XRP")
      .assetScale((short) 9)
      .subject("wallet.com$foo")
      .expiresAt(Instant.MAX)
      .received(UnsignedLong.valueOf(1000))
      .description("Test invoice")
      .build();

    when(invoiceServiceMock.createInvoice(any())).thenThrow(new InvalidInvoiceSubjectProblem(invoiceMock.subject()));

    mockMvc
      .perform(post(OpenPaymentsPathConstants.SLASH_INVOICE)
        .headers(this.testJsonHeaders())
        .content(objectMapper.writeValueAsString(invoiceMock))
      )
      .andExpect(status().isBadRequest());
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
      .perform(options(OpenPaymentsPathConstants.SLASH_INVOICE + PathConstants.SLASH + invoiceId)
        .headers(this.testJsonHeaders())
//        .with(httpBasic("admin", "password")).with(csrf())
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.destination_account").value(destinationAddress.getValue() + "~" + encodedInvoiceId));
  }

}
