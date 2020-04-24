package org.interledger.connector.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.InvoiceId;
import org.interledger.connector.opa.model.OpenPaymentsSettings;
import org.interledger.connector.opa.model.PaymentNetwork;
import org.interledger.connector.opa.model.problems.InvalidInvoiceSubjectProblem;
import org.interledger.connector.opa.model.problems.InvoiceNotFoundProblem;
import org.interledger.connector.persistence.entities.InvoiceEntity;
import org.interledger.connector.persistence.repositories.InvoicesRepository;
import org.interledger.core.InterledgerAddress;
import org.interledger.spsp.PaymentPointerResolver;

import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.springframework.core.convert.ConversionService;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class DefaultInvoiceServiceTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private ConversionService conversionService;

  @Mock
  private InvoicesRepository invoicesRepositoryMock;

  private DefaultInvoiceService defaultInvoiceService;



  @Before
  public void setUp() {

    initMocks(this);

    defaultInvoiceService = new DefaultInvoiceService(
      invoicesRepositoryMock,
      conversionService);
  }

  @Test
  public void getExistingIlpInvoice() {
    Invoice invoiceMock = Invoice.builder()
      .accountId("foo")
      .amount(UnsignedLong.valueOf(1000))
      .assetCode("XRP")
      .assetScale((short) 9)
      .subject("$wallet.com/foo")
      .expiresAt(Instant.MAX)
      .received(UnsignedLong.valueOf(1000))
      .build();

    when(invoicesRepositoryMock.findInvoiceByInvoiceId(eq(invoiceMock.id()))).thenReturn(Optional.of(invoiceMock));
    Invoice invoiceReturned = defaultInvoiceService.getInvoiceById(invoiceMock.id());
    assertThat(invoiceMock).isEqualTo(invoiceReturned);
  }

  @Test
  public void getNonExistentInvoice() {
    when(invoicesRepositoryMock.findInvoiceByInvoiceId(any())).thenReturn(Optional.empty());
    expectedException.expect(InvoiceNotFoundProblem.class);
    defaultInvoiceService.getInvoiceById(InvoiceId.of(UUID.randomUUID().toString()));
  }

  @Test
  public void createValidIlpInvoice() {
    Invoice invoice = Invoice.builder()
      .accountId("foo")
      .amount(UnsignedLong.valueOf(1000))
      .assetCode("XRP")
      .assetScale((short) 9)
      .subject("$wallet.com/p/foo")
      .expiresAt(Instant.MAX)
      .received(UnsignedLong.valueOf(1000))
      .build();

    when(invoicesRepositoryMock.saveInvoice(eq(invoice))).thenReturn(invoice);
    Invoice createdInvoice = defaultInvoiceService.createInvoice(invoice);
    assertThat(invoice).isEqualTo(createdInvoice);
  }

  /*@Test
  public void createInvoiceWithInvalidSubjectNoPath() {
    Invoice invoice = Invoice.builder()
      .accountId("foo")
      .amount(UnsignedLong.valueOf(1000))
      .assetCode("XRP")
      .assetScale((short) 9)
      .subject("$wallet.com/p")
      .expiresAt(Instant.MAX)
      .received(UnsignedLong.valueOf(1000))
      .build();

    expectedException.expect(InvalidInvoiceSubjectProblem.class);
    expectedException.expectMessage("Invoice subject did not include user identifying information.");
    defaultInvoiceService.createInvoice(invoice);
  }*/

  @Test
  public void updateInvoiceIlpWithNewReceivedAmount() {
    InvoiceId invoiceId = InvoiceId.of(UUID.randomUUID().toString());

    InvoiceEntity originalInvoiceEntity = new InvoiceEntity(
      Invoice.builder()
        .id(invoiceId)
        .accountId("foo")
        .amount(UnsignedLong.valueOf(1000))
        .assetCode("XRP")
        .assetScale((short) 9)
        .subject("$wallet.com/p")
        .expiresAt(Instant.MAX)
        .received(UnsignedLong.valueOf(0))
        .build()
    );

    Invoice invoiceToUpdate = Invoice.builder()
      .id(invoiceId)
      .accountId("foo")
      .amount(UnsignedLong.valueOf(1000))
      .assetCode("XRP")
      .assetScale((short) 9)
      .subject("$wallet.com/p")
      .expiresAt(Instant.MAX)
      .received(UnsignedLong.valueOf(1000))
      .build();

    InvoiceEntity updatedInvoiceEntity = new InvoiceEntity(invoiceToUpdate);

    when(invoicesRepositoryMock.findByInvoiceId(eq(invoiceId))).thenReturn(Optional.of(originalInvoiceEntity));
    when(conversionService.convert(eq(updatedInvoiceEntity), eq(Invoice.class))).thenReturn(invoiceToUpdate);

    Invoice updatedInvoice = defaultInvoiceService.updateInvoice(invoiceToUpdate);
    assertThat(updatedInvoice).isEqualTo(invoiceToUpdate);
  }

  @Test
  public void updateIlpInvoiceNotFound() {
    InvoiceId invoiceId = InvoiceId.of(UUID.randomUUID().toString());
    Invoice invoiceMock = mock(Invoice.class);
    when(invoiceMock.id()).thenReturn(invoiceId);

    when(invoicesRepositoryMock.findByInvoiceId(eq(invoiceId))).thenReturn(Optional.empty());
    expectedException.expect(InvoiceNotFoundProblem.class);
    defaultInvoiceService.updateInvoice(invoiceMock);
  }

  @Test
  public void getExistingXrpInvoice() {
    Invoice invoiceMock = Invoice.builder()
      .accountId("foo")
      .amount(UnsignedLong.valueOf(1000))
      .assetCode("XRP")
      .paymentNetwork(PaymentNetwork.XRPL)
      .assetScale((short) 9)
      .subject("foo$wallet.com")
      .expiresAt(Instant.MAX)
      .received(UnsignedLong.valueOf(1000))
      .build();

    when(invoicesRepositoryMock.findInvoiceByInvoiceId(eq(invoiceMock.id()))).thenReturn(Optional.of(invoiceMock));
    Invoice invoiceReturned = defaultInvoiceService.getInvoiceById(invoiceMock.id());
    assertThat(invoiceMock).isEqualTo(invoiceReturned);
  }

  @Test
  public void createValidXrpInvoice() {
    Invoice invoice = Invoice.builder()
      .accountId("foo")
      .amount(UnsignedLong.valueOf(1000))
      .assetCode("XRP")
      .assetScale((short) 9)
      .subject("foo$wallet.com")
      .expiresAt(Instant.MAX)
      .received(UnsignedLong.valueOf(1000))
      .paymentNetwork(PaymentNetwork.XRPL)
      .build();

    when(invoicesRepositoryMock.saveInvoice(eq(invoice))).thenReturn(invoice);
    Invoice createdInvoice = defaultInvoiceService.createInvoice(invoice);
    assertThat(invoice).isEqualTo(createdInvoice);
  }

  /*@Test
  public void createInvoiceWithInvalidSubjectNoPath() {
    Invoice invoice = Invoice.builder()
      .accountId("foo")
      .amount(UnsignedLong.valueOf(1000))
      .assetCode("XRP")
      .assetScale((short) 9)
      .subject("$wallet.com/p")
      .expiresAt(Instant.MAX)
      .received(UnsignedLong.valueOf(1000))
      .build();

    expectedException.expect(InvalidInvoiceSubjectProblem.class);
    expectedException.expectMessage("Invoice subject did not include user identifying information.");
    defaultInvoiceService.createInvoice(invoice);
  }*/

  @Test
  public void updateInvoiceXrpWithNewReceivedAmount() {
    InvoiceId invoiceId = InvoiceId.of(UUID.randomUUID().toString());

    InvoiceEntity originalInvoiceEntity = new InvoiceEntity(
      Invoice.builder()
        .id(invoiceId)
        .accountId("foo")
        .amount(UnsignedLong.valueOf(1000))
        .assetCode("XRP")
        .assetScale((short) 9)
        .subject("foo$wallet.com")
        .expiresAt(Instant.MAX)
        .received(UnsignedLong.valueOf(0))
        .paymentNetwork(PaymentNetwork.XRPL)
        .build()
    );

    Invoice invoiceToUpdate = Invoice.builder()
      .id(invoiceId)
      .accountId("foo")
      .amount(UnsignedLong.valueOf(1000))
      .assetCode("XRP")
      .assetScale((short) 9)
      .subject("foo$wallet.com")
      .expiresAt(Instant.MAX)
      .received(UnsignedLong.valueOf(1000))
      .paymentNetwork(PaymentNetwork.XRPL)
      .build();

    InvoiceEntity updatedInvoiceEntity = new InvoiceEntity(invoiceToUpdate);

    when(invoicesRepositoryMock.findByInvoiceId(eq(invoiceId))).thenReturn(Optional.of(originalInvoiceEntity));
    when(conversionService.convert(eq(updatedInvoiceEntity), eq(Invoice.class))).thenReturn(invoiceToUpdate);

    Invoice updatedInvoice = defaultInvoiceService.updateInvoice(invoiceToUpdate);
    assertThat(updatedInvoice).isEqualTo(invoiceToUpdate);
  }
}
