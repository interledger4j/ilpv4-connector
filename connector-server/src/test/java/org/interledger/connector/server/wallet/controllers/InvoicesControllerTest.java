package org.interledger.connector.server.wallet.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.InvoiceId;
import org.interledger.connector.opa.model.OpenPaymentsMetadata;
import org.interledger.connector.opa.model.OpenPaymentsSettings;
import org.interledger.connector.opa.model.PaymentNetwork;
import org.interledger.connector.opa.model.XrpPaymentDetails;
import org.interledger.connector.opa.model.problems.InvoiceNotFoundProblem;
import org.interledger.connector.server.spring.controllers.AbstractControllerTest;
import org.interledger.connector.server.spring.controllers.PathConstants;
import org.interledger.connector.settings.properties.OpenPaymentsPathConstants;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.SharedSecret;
import org.interledger.spsp.StreamConnectionDetails;
import org.interledger.stream.crypto.Random;

import com.google.common.primitives.UnsignedLong;
import feign.FeignException;
import okhttp3.HttpUrl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;
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
  public void getExistingInvoice() throws Exception {
    Invoice invoiceMock = Invoice.builder()
      .accountId("foo")
      .amount(UnsignedLong.valueOf(1000))
      .assetCode("XRP")
      .assetScale((short) 9)
      .subject("$localhost:8080/foo")
      .expiresAt(Instant.MAX)
      .received(UnsignedLong.valueOf(1000))
      .description("Test invoice")
      .build();

    when(invoiceServiceMock.getInvoiceById(invoiceMock.id())).thenReturn(invoiceMock);

    mockMvc
      .perform(get("/foo" + OpenPaymentsPathConstants.SLASH_INVOICES + PathConstants.SLASH + invoiceMock.id())
        .headers(this.testJsonHeaders()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.amount").value(invoiceMock.amount().longValue()))
      .andExpect(jsonPath("$.assetCode").value(invoiceMock.assetCode()))
      .andExpect(jsonPath("$.assetScale").value((int) invoiceMock.assetScale()))
      .andExpect(jsonPath("$.subject").value(invoiceMock.subject()))
      .andExpect(jsonPath("$.expiresAt").value(invoiceMock.expiresAt().toString()))
      .andExpect(jsonPath("$.received").value(invoiceMock.received().longValue()))
      .andExpect(jsonPath("$.description").value(invoiceMock.description()))
      .andExpect(jsonPath("$.name").value(invoiceMock.invoiceUrl().toString()));
  }

  @Test
  public void getExistingPaidInvoiceReceipt() throws Exception {
    Invoice paidInvoice = Invoice.builder()
      .accountId("foo")
      .amount(UnsignedLong.valueOf(1000))
      .assetCode("XRP")
      .assetScale((short) 9)
      .subject("$wallet.com/foo") // We are not the invoice owner
      .expiresAt(Instant.MAX)
      .received(UnsignedLong.valueOf(1000)) // paid
      .description("Test invoice")
      .build();

    when(invoiceServiceMock.getInvoiceById(eq(paidInvoice.id()))).thenReturn(paidInvoice);

    mockMvc
      .perform(get("/foo" + OpenPaymentsPathConstants.SLASH_INVOICES + PathConstants.SLASH + paidInvoice.id())
        .headers(this.testJsonHeaders()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.amount").value(paidInvoice.amount().longValue()))
      .andExpect(jsonPath("$.assetCode").value(paidInvoice.assetCode()))
      .andExpect(jsonPath("$.assetScale").value((int) paidInvoice.assetScale()))
      .andExpect(jsonPath("$.subject").value(paidInvoice.subject()))
      .andExpect(jsonPath("$.expiresAt").value(paidInvoice.expiresAt().toString()))
      .andExpect(jsonPath("$.received").value(paidInvoice.received().longValue()))
      .andExpect(jsonPath("$.description").value(paidInvoice.description()));
  }

  @Test
  public void getExistingUnpaidInvoiceReceipt() throws Exception {
    Invoice unpaidInvoice = Invoice.builder()
      .accountId("foo")
      .amount(UnsignedLong.valueOf(1000))
      .assetCode("XRP")
      .assetScale((short) 9)
      .subject("$wallet.com/foo")
      .expiresAt(Instant.MAX)
      .received(UnsignedLong.ZERO) // Not paid
      .description("Test invoice")
      .build();

    Invoice paidInvoice = Invoice.builder()
      .from(unpaidInvoice)
      .received(unpaidInvoice.amount())
      .build();

    when(invoiceServiceMock.getInvoiceById(eq(unpaidInvoice.id()))).thenReturn(unpaidInvoice);

    when(openPaymentsClientMock.getInvoice(eq(unpaidInvoice.invoiceUrl().get().uri()))).thenReturn(paidInvoice);
    when(invoiceServiceMock.updateOrCreateInvoice(eq(paidInvoice))).thenReturn(paidInvoice);

    mockMvc
      .perform(get("/foo" + OpenPaymentsPathConstants.SLASH_INVOICES + PathConstants.SLASH + paidInvoice.id())
        .headers(this.testJsonHeaders()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.amount").value(paidInvoice.amount().longValue()))
      .andExpect(jsonPath("$.assetCode").value(paidInvoice.assetCode()))
      .andExpect(jsonPath("$.assetScale").value((int) paidInvoice.assetScale()))
      .andExpect(jsonPath("$.subject").value(paidInvoice.subject()))
      .andExpect(jsonPath("$.expiresAt").value(paidInvoice.expiresAt().toString()))
      .andExpect(jsonPath("$.received").value(paidInvoice.received().longValue()))
      .andExpect(jsonPath("$.description").value(paidInvoice.description()));
  }

  /*@Test
  public void getExistingInvoiceWithUpdate() throws Exception {
    Invoice invoiceMock = Invoice.builder()
      .accountId("$otherwallet.com/foo")
      .amount(UnsignedLong.valueOf(1000))
      .assetCode("XRP")
      .assetScale((short) 9)
      .subject("$wallet.com/foo")
      .expiresAt(Instant.MAX)
      .received(UnsignedLong.ZERO) // Not paid
      .description("Test invoice")
      .build();

    Invoice updatedInvoiceMock = Invoice.builder()
      .accountId("$otherwallet.com/foo")
      .amount(UnsignedLong.valueOf(1000))
      .assetCode("XRP")
      .assetScale((short) 9)
      .subject("$wallet.com/foo")
      .expiresAt(Instant.MAX)
      .received(UnsignedLong.valueOf(1000)) // Not paid
      .description("Test invoice")
      .build();

    when(invoiceServiceMock.getInvoiceById(invoiceMock.id())).thenReturn(invoiceMock);
    OpenPaymentsMetadata mockMetadata = mock(OpenPaymentsMetadata.class);
    when(openPaymentsClientMock.getMetadata(any())).thenReturn(mockMetadata);
    HttpUrl receiverUrl = HttpUrl.parse("https://wallet.com/invoices");
    when(mockMetadata.invoicesEndpoint()).thenReturn(receiverUrl);
    when(openPaymentsClientMock.getInvoice(eq(receiverUrl.uri()), eq(invoiceMock.id().value())))
      .thenReturn(updatedInvoiceMock);
    when(invoiceServiceMock.updateInvoice(eq(updatedInvoiceMock))).thenReturn(updatedInvoiceMock);

    mockMvc
      .perform(get(OpenPaymentsPathConstants.SLASH_INVOICES + PathConstants.SLASH + invoiceMock.id())
        .headers(this.testJsonHeaders()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.accountId").value(updatedInvoiceMock.accountId().get()))
      .andExpect(jsonPath("$.amount").value(updatedInvoiceMock.amount().longValue()))
      .andExpect(jsonPath("$.assetCode").value(updatedInvoiceMock.assetCode()))
      .andExpect(jsonPath("$.assetScale").value((int) updatedInvoiceMock.assetScale()))
      .andExpect(jsonPath("$.subject").value(updatedInvoiceMock.subject()))
      .andExpect(jsonPath("$.expiresAt").value(updatedInvoiceMock.expiresAt().toString()))
      .andExpect(jsonPath("$.received").value(updatedInvoiceMock.received().longValue()))
      .andExpect(jsonPath("$.description").value(updatedInvoiceMock.description()))
      .andExpect(jsonPath("$.paymentId").hasJsonPath());
  }*/

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
      .perform(get(OpenPaymentsPathConstants.SLASH_INVOICES + PathConstants.SLASH + invoiceMock.id())
        .headers(this.testJsonHeaders()))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.type").value("https://errors.interledger.org/invoices/invoice-not-found"));
  }

  /*@Test
  public void createValidInvoice() throws Exception {
    Invoice invoiceMock = Invoice.builder()
      .accountId("foo")
      .amount(UnsignedLong.valueOf(1000))
      .assetCode("XRP")
      .assetScale((short) 9)
      .subject("$wallet.com/foo")
//      .expiresAt(Instant.MAX)
      .received(UnsignedLong.valueOf(1000))
      .description("Test invoice")
      .build();

    when(invoiceServiceMock.createInvoice(any())).thenReturn(invoiceMock);

    mockMvc
      .perform(post("/foo")
        .headers(this.testInvoicePostHeaders())
        .content(objectMapper.writeValueAsString(invoiceMock))
      )
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.accountId").value(invoiceMock.accountId().get()))
      .andExpect(jsonPath("$.amount").value(invoiceMock.amount().longValue()))
      .andExpect(jsonPath("$.assetCode").value(invoiceMock.assetCode()))
      .andExpect(jsonPath("$.assetScale").value((int) invoiceMock.assetScale()))
      .andExpect(jsonPath("$.subject").value(invoiceMock.subject()))
      .andExpect(jsonPath("$.expiresAt").value(invoiceMock.expiresAt().toString()))
      .andExpect(jsonPath("$.received").value(invoiceMock.received().longValue()))
      .andExpect(jsonPath("$.description").value(invoiceMock.description()))
      .andExpect(jsonPath("$.paymentId").hasJsonPath());
  }*/

  /*@Test
  public void getPaymentDetailsForOwnIlpInvoice() throws Exception {
    InvoiceId invoiceId = InvoiceId.of("66ce60d8-f4ba-4c60-ba6e-fc5e0aa99923");
    String encodedInvoiceId = "NjZjZTYwZDgtZjRiYS00YzYwLWJhNmUtZmM1ZTBhYTk5OTIz";

    Invoice mockInvoice = mock(Invoice.class);
    when(invoiceServiceMock.getInvoiceById(invoiceId)).thenReturn(mockInvoice);
    when(mockInvoice.subject()).thenReturn("$foo.bar/baz");
    when(mockInvoice.accountId()).thenReturn(Optional.of("$foo.bar/baz"));
    when(mockInvoice.paymentNetwork()).thenReturn(PaymentNetwork.ILP);
    when(mockInvoice.paymentId()).thenReturn(encodedInvoiceId);
    when(ilpPaymentDetailsService.getAddressFromInvoiceSubject(mockInvoice.subject())).thenReturn("test.foo.bar.123456");

    InterledgerAddress destinationAddress = InterledgerAddress.of("test.foo.bar.123456");
    StreamConnectionDetails streamConnectionDetails = StreamConnectionDetails.builder()
      .destinationAddress(destinationAddress)
      .sharedSecret(SharedSecret.of(Random.randBytes(32)))
      .build();

    when(streamConnectionGeneratorMock.generateConnectionDetails(serverSecretSupplierMock, destinationAddress))
      .thenReturn(streamConnectionDetails);

    mockMvc
      .perform(options(OpenPaymentsPathConstants.SLASH_INVOICES + PathConstants.SLASH + invoiceId)
        .headers(this.testJsonHeaders())
//        .with(httpBasic("admin", "password")).with(csrf())
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.destination_account").value(destinationAddress.getValue() + "~" + encodedInvoiceId));
  }

  @Test
  public void getPaymentDetailsForReceiverIlpInvoice() throws Exception {
    InvoiceId invoiceId = InvoiceId.of("66ce60d8-f4ba-4c60-ba6e-fc5e0aa99923");
    String encodedInvoiceId = "NjZjZTYwZDgtZjRiYS00YzYwLWJhNmUtZmM1ZTBhYTk5OTIz";

    Invoice mockInvoice = mock(Invoice.class);
    when(invoiceServiceMock.getInvoiceById(invoiceId)).thenReturn(mockInvoice);
    when(mockInvoice.subject()).thenReturn("$foo.bar/baz");
    when(mockInvoice.accountId()).thenReturn(Optional.of("$example.com/foo"));
    when(mockInvoice.paymentNetwork()).thenReturn(PaymentNetwork.ILP);
    when(mockInvoice.id()).thenReturn(invoiceId);

    OpenPaymentsMetadata mockMetadata = mock(OpenPaymentsMetadata.class);
    when(openPaymentsClientMock.getMetadata(any())).thenReturn(mockMetadata);

    InterledgerAddress destinationAddress = InterledgerAddress.of("test.foo.bar.123456");
    StreamConnectionDetails streamConnectionDetails = StreamConnectionDetails.builder()
      .destinationAddress(InterledgerAddress.of(destinationAddress.getValue() + "~" + encodedInvoiceId))
      .sharedSecret(SharedSecret.of(Random.randBytes(32)))
      .build();


    HttpUrl invoicesEndpoint = new HttpUrl.Builder()
      .scheme("https")
      .host("foo.bar")
      .addPathSegment("invoices")
      .build();

    when(openPaymentsClientMock.getIlpInvoicePaymentDetails(eq(invoicesEndpoint.uri()), eq(invoiceId.value())))
      .thenReturn(streamConnectionDetails);

    when(mockMetadata.invoicesEndpoint()).thenReturn(invoicesEndpoint);

    mockMvc
      .perform(options(OpenPaymentsPathConstants.SLASH_INVOICES + PathConstants.SLASH + invoiceId)
          .headers(this.testJsonHeaders())
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.destination_account").value(destinationAddress.getValue() + "~" + encodedInvoiceId));
  }

  @Test
  public void getXrpPaymentDetailsForOwnInvoice() throws Exception {
    InvoiceId invoiceId = InvoiceId.of("66ce60d8-f4ba-4c60-ba6e-fc5e0aa99923");
    String destinationTag = "1234";

    Invoice mockInvoice = mock(Invoice.class);
    when(invoiceServiceMock.getInvoiceById(invoiceId)).thenReturn(mockInvoice);
    when(mockInvoice.subject()).thenReturn("foo$example.com");
    when(mockInvoice.accountId()).thenReturn(Optional.of("foo$example.com"));
    when(mockInvoice.paymentNetwork()).thenReturn(PaymentNetwork.XRPL);
    when(mockInvoice.paymentId()).thenReturn(destinationTag);
    String destinationAddress = "afieuwnfasiudhfqepqjnecvapjnsd";
    when(xrpPaymentDetailsService.getAddressFromInvoiceSubject(mockInvoice.subject())).thenReturn(destinationAddress); // I'm aware this isnt an XRP address...

    mockMvc
      .perform(options(OpenPaymentsPathConstants.SLASH_INVOICES + PathConstants.SLASH + invoiceId)
          .headers(this.testJsonHeaders())
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.address").value(destinationAddress))
      .andExpect(jsonPath("$.invoiceIdHash").value(Integer.valueOf(destinationTag)));
  }

  @Test
  public void getXrpPaymentDetailsForReceiverInvoice() throws Exception {
    InvoiceId invoiceId = InvoiceId.of("66ce60d8-f4ba-4c60-ba6e-fc5e0aa99923");

    Invoice mockInvoice = mock(Invoice.class);
    when(invoiceServiceMock.getInvoiceById(invoiceId)).thenReturn(mockInvoice);
    when(mockInvoice.subject()).thenReturn("foo$example.com");
    when(mockInvoice.accountId()).thenReturn(Optional.of("foo$anotherexample.com"));
    when(mockInvoice.paymentNetwork()).thenReturn(PaymentNetwork.XRPL);
    when(mockInvoice.id()).thenReturn(invoiceId);

    OpenPaymentsMetadata mockMetadata = mock(OpenPaymentsMetadata.class);
    when(openPaymentsClientMock.getMetadata(any())).thenReturn(mockMetadata);

    HttpUrl invoicesEndpoint = new HttpUrl.Builder()
      .scheme("https")
      .host("example.com")
      .addPathSegment("invoices")
      .build();

    when(mockMetadata.invoicesEndpoint()).thenReturn(invoicesEndpoint);

    String destinationAddress = "afieuwnfasiudhfqepqjnecvapjnsd";
    String destinationTag = "123456";
    XrpPaymentDetails xrpPaymentDetails = XrpPaymentDetails.builder()
      .address(destinationAddress)
      .invoiceIdHash(destinationTag)
      .build();
    when(openPaymentsClientMock.getXrpInvoicePaymentDetails(eq(invoicesEndpoint.uri()), eq(invoiceId.value())))
      .thenReturn(xrpPaymentDetails);


    mockMvc
      .perform(options(OpenPaymentsPathConstants.SLASH_INVOICES + PathConstants.SLASH + invoiceId)
        .headers(this.testJsonHeaders())
      )
      .andExpect(jsonPath("$.address").value(destinationAddress))
      .andExpect(jsonPath("$.invoiceIdHash").value(destinationTag));
  }*/
}
