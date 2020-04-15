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

public class IlpInvoiceServiceTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private OpenPaymentsSettings openPaymentsSettingsMock;

  @Mock
  private ConversionService conversionService;

  @Mock
  private InvoicesRepository invoicesRepositoryMock;

  private PaymentPointerResolver paymentPointerResolver;

  private String opaUrlPath;

  private IlpInvoiceService ilpInvoiceService;

  private InterledgerAddress operatorAddress;

  @Before
  public void setUp() {

    initMocks(this);

    operatorAddress = InterledgerAddress.of("test.jc1");
    when(openPaymentsSettingsMock.ilpOperatorAddress()).thenReturn(operatorAddress);

    paymentPointerResolver = PaymentPointerResolver.defaultResolver();

    opaUrlPath = "/p/";
    ilpInvoiceService = new IlpInvoiceService(
      () -> openPaymentsSettingsMock,
      paymentPointerResolver,
      opaUrlPath,
      invoicesRepositoryMock,
      conversionService);
  }

  @Test
  public void getExistingInvoice() {
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
    Invoice invoiceReturned = ilpInvoiceService.getInvoiceById(invoiceMock.id());
    assertThat(invoiceMock).isEqualTo(invoiceReturned);
  }

  @Test
  public void getNonExistentInvoice() {
    when(invoicesRepositoryMock.findInvoiceByInvoiceId(any())).thenReturn(Optional.empty());
    expectedException.expect(InvoiceNotFoundProblem.class);
    ilpInvoiceService.getInvoiceById(InvoiceId.of(UUID.randomUUID().toString()));
  }

  @Test
  public void createValidInvoice() {
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
    Invoice createdInvoice = ilpInvoiceService.createInvoice(invoice);
    assertThat(invoice).isEqualTo(createdInvoice);
  }

  @Test
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
    ilpInvoiceService.createInvoice(invoice);
  }

  @Test
  public void createInvoiceWithInvalidSubjectPaymentPointer() {
    Invoice invoice = Invoice.builder()
      .accountId("foo")
      .amount(UnsignedLong.valueOf(1000))
      .assetCode("XRP")
      .assetScale((short) 9)
      .subject("wallet.com/p")
      .expiresAt(Instant.MAX)
      .received(UnsignedLong.valueOf(1000))
      .build();

    expectedException.expect(InvalidInvoiceSubjectProblem.class);
    expectedException.expectMessage("Invoice subject was an invalid Payment Pointer.");
    ilpInvoiceService.createInvoice(invoice);
  }

  @Test
  public void getAddressFromInvoiceSubject() {
    String subjectPaymentPointer = "$xpring.money/p/foo";

    InterledgerAddress operatorAddress = InterledgerAddress.of("test.jc1");
    when(openPaymentsSettingsMock.ilpOperatorAddress()).thenReturn(operatorAddress);

    String resolvedAddress = ilpInvoiceService.getAddressFromInvoiceSubject(subjectPaymentPointer);
    assertThat(resolvedAddress).isEqualTo(operatorAddress.with("foo").getValue());
  }

  @Test
  public void getAddressFromInvoiceSubjectWithNoOpaPath() {
    ilpInvoiceService = new IlpInvoiceService(
      () -> openPaymentsSettingsMock,
      paymentPointerResolver,
      "",
      invoicesRepositoryMock,
      conversionService);

    String subjectPaymentPointer = "$xpring.money/foo";

    String resolvedAddress = ilpInvoiceService.getAddressFromInvoiceSubject(subjectPaymentPointer);
    assertThat(resolvedAddress).isEqualTo(operatorAddress.with("foo").getValue());
  }

  @Test
  public void getAddressFromInvalidInvoiceSubject() {
    String subjectPaymentPointer = "$xpring.money";

    InterledgerAddress operatorAddress = InterledgerAddress.of("test.jc1");
    when(openPaymentsSettingsMock.ilpOperatorAddress()).thenReturn(operatorAddress);

    expectedException.expect(InvalidInvoiceSubjectProblem.class);
    ilpInvoiceService.getAddressFromInvoiceSubject(subjectPaymentPointer);
  }

  @Test
  public void updateInvoiceWithNewReceivedAmount() {
    InvoiceId invoiceId = InvoiceId.of(UUID.randomUUID().toString());

    InvoiceEntity originalInvoiceEntity = new InvoiceEntity(
      Invoice.builder()
        .id(invoiceId)
        .accountId("foo")
        .amount(UnsignedLong.valueOf(1000))
        .assetCode("XRP")
        .assetScale((short) 9)
        .subject("wallet.com/p")
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
      .subject("wallet.com/p")
      .expiresAt(Instant.MAX)
      .received(UnsignedLong.valueOf(1000))
      .build();

    InvoiceEntity updatedInvoiceEntity = new InvoiceEntity(invoiceToUpdate);

    when(invoicesRepositoryMock.findByInvoiceId(eq(invoiceId))).thenReturn(Optional.of(originalInvoiceEntity));
    when(conversionService.convert(eq(updatedInvoiceEntity), eq(Invoice.class))).thenReturn(invoiceToUpdate);

    Invoice updatedInvoice = ilpInvoiceService.updateInvoice(invoiceToUpdate);
    assertThat(updatedInvoice).isEqualTo(invoiceToUpdate);
  }

  @Test
  public void updateInvoiceNotFound() {
    InvoiceId invoiceId = InvoiceId.of(UUID.randomUUID().toString());
    Invoice invoiceMock = mock(Invoice.class);
    when(invoiceMock.id()).thenReturn(invoiceId);

    when(invoicesRepositoryMock.findByInvoiceId(eq(invoiceId))).thenReturn(Optional.empty());
    expectedException.expect(InvoiceNotFoundProblem.class);
    ilpInvoiceService.updateInvoice(invoiceMock);
  }
}
