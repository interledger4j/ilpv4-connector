package org.interledger.connector.server.wallet.controllers;

import static org.interledger.connector.core.ConfigConstants.ENABLED_PROTOCOLS;
import static org.interledger.connector.core.ConfigConstants.SPSP_ENABLED;
import static org.interledger.connector.core.ConfigConstants.TRUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.interledger.connector.opa.InvoiceService;
import org.interledger.connector.opa.PaymentDetailsService;
import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.InvoiceId;
import org.interledger.connector.opa.model.OpenPaymentsSettings;
import org.interledger.connector.opa.model.XrpPayment;
import org.interledger.connector.opa.model.problems.InvoiceNotFoundProblem;
import org.interledger.connector.payments.StreamPayment;
import org.interledger.connector.settings.properties.OpenPaymentsPathConstants;
import org.interledger.connector.wallet.OpenPaymentsClient;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.PaymentPointerResolver;
import org.interledger.stream.receiver.ServerSecretSupplier;
import org.interledger.stream.receiver.StreamConnectionGenerator;

import feign.FeignException;
import okhttp3.HttpUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.zalando.problem.spring.common.MediaTypes;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

@RestController
// TODO make an enabled property for open payments?
@ConditionalOnProperty(prefix = ENABLED_PROTOCOLS, name = SPSP_ENABLED, havingValue = TRUE)
public class InvoicesController {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private InvoiceService invoiceService;
  private final Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier;
  private final ServerSecretSupplier serverSecretSupplier;
  private final PaymentDetailsService ilpPaymentDetailsService;
  private final PaymentDetailsService xrpPaymentDetailsService;
  private final StreamConnectionGenerator streamConnectionGenerator;
  private final OpenPaymentsClient openPaymentsClient;
  private final PaymentPointerResolver paymentPointerResolver;

  public InvoicesController(
    final InvoiceService invoiceService,
    final PaymentDetailsService ilpPaymentDetailsService,
    final PaymentDetailsService xrpPaymentDetailsService,
    final Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier,
    final ServerSecretSupplier serverSecretSupplier,
    final StreamConnectionGenerator streamConnectionGenerator,
    final OpenPaymentsClient openPaymentsClient,
    final PaymentPointerResolver paymentPointerResolver
    ) {
    this.invoiceService = Objects.requireNonNull(invoiceService);
    this.ilpPaymentDetailsService = Objects.requireNonNull(ilpPaymentDetailsService);
    this.xrpPaymentDetailsService = Objects.requireNonNull(xrpPaymentDetailsService);
    this.streamConnectionGenerator = Objects.requireNonNull(streamConnectionGenerator);
    this.openPaymentsSettingsSupplier = Objects.requireNonNull(openPaymentsSettingsSupplier);
    this.serverSecretSupplier = Objects.requireNonNull(serverSecretSupplier);
    this.openPaymentsClient = Objects.requireNonNull(openPaymentsClient);
    this.paymentPointerResolver = Objects.requireNonNull(paymentPointerResolver);
  }

  /**
   * Create and return an Invoice on the Open Payments server.
   *
   * An Invoice is created after it is stored in a data store.
   *
   * @param invoice An {@link Invoice} to create on the Open Payments server.
   * @return A 201 Created if successful, and the fully populated {@link Invoice} which was stored.
   */
  @RequestMapping(
    path = OpenPaymentsPathConstants.SLASH_ACCOUNT_ID + OpenPaymentsPathConstants.SLASH_INVOICES,
    method = RequestMethod.POST,
    produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public @ResponseBody ResponseEntity<Invoice> createInvoice(@RequestBody Invoice invoice) {
    Invoice createdInvoice = invoiceService.createInvoice(invoice);

    final HttpHeaders headers = new HttpHeaders();
    headers.setLocation(getInvoiceLocation(invoice.id()));
    return new ResponseEntity(createdInvoice, headers, HttpStatus.CREATED);
  }

  /**
   * Get an existing {@link Invoice}. If an invoice URL is provided as a request parameter, this will get an
   * invoice receipt, otherwise it will assume that we are the invoice owner and just fetch from our records.
   *
   * @param invoiceUrl The location of the original {@link Invoice}. If requesting an invoice that this server owns,
   *                   this is not necessary.
   * @param invoiceId The {@link InvoiceId} of the {@link Invoice} being retrieved.
   * @return An existing {@link Invoice}
   */
  // https://xpring.io/foo/invoices/1234?invoiceUrl=https://rafiki.money/invoices/1234
  @RequestMapping(
    path = OpenPaymentsPathConstants.SLASH_ACCOUNT_ID + OpenPaymentsPathConstants.SLASH_INVOICES + OpenPaymentsPathConstants.SLASH_INVOICE_ID,
    method = RequestMethod.GET,
    produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public @ResponseBody Invoice getInvoice(
    @RequestParam(name = "invoiceUrl", required = false) String invoiceUrl,
    @PathVariable(name = OpenPaymentsPathConstants.INVOICE_ID) InvoiceId invoiceId
  ) {
    if (invoiceUrl != null) {
      return this.getInvoiceReceipt(HttpUrl.parse(invoiceUrl), invoiceId);
    }
    return invoiceService.getInvoiceById(invoiceId);
  }

  /**
   * Get an existing invoice receipt.  An invoice receipt is the sender wallet's copy of an {@link Invoice}.
   * If we have a record of a paid invoice, we can just return that. If the invoice hasn't been fully paid,
   * this endpoint will request an update from the invoice owner and update the invoice in our own records.
   *
   * @param invoiceUrl The location of the {@link Invoice}.
   * @return An updated view of an {@link Invoice} which is owned by another wallet.
   * @throws Exception
   */
  public Invoice getInvoiceReceipt(
    HttpUrl invoiceUrl,
    InvoiceId invoiceId
  ) {
    // The sender and receiver wallets could be the same wallet, so the invoice and the invoice receipt could be
    // the same record. In this case, we should just return what we have.
    if (ServletUriComponentsBuilder.fromCurrentContextPath().build().getHost().equals(invoiceUrl.host())) {
      return invoiceService.getInvoiceById(invoiceId);
    }

    // Otherwise, we can assume that some other wallet is the invoice owner.
    try {
      // Look at our own records. If, from our view, the invoice has been paid, no need to reach out to the receiver to
      // update it.
      Invoice existingInvoiceReceipt = invoiceService.getInvoiceById(invoiceId);
      if (existingInvoiceReceipt.isPaid()) {
        return existingInvoiceReceipt;
      } else {
        throw new Exception("Local invoice receipt has not been fully paid. Request an updated invoice from the invoice owner.");
      }
    } catch (Exception e) {
      // If our copy of the invoice hasn't been paid, or we don't have a record of that invoice, try
      // to reach out to the invoice owner to update/create our record
      try {
        Invoice invoiceOnReceiver = openPaymentsClient.getInvoice(invoiceUrl.uri());
        return invoiceService.updateOrCreateInvoice(invoiceOnReceiver);
      } catch (FeignException fe) {
        if (fe.status() == 404) {
          throw new InvoiceNotFoundProblem("Original invoice was not found in invoice owner's records.", invoiceId);
        }

        throw new RuntimeException(e);
      }
    }
  }

  /**
   * Endpoint to notify the Open Payments Server that an XRP payment has been received on the XRPL.
   *
   * The Open Payments Server will then decode the destination tag of the receiver's address and determine
   * if the payment was meant for an Invoice.
   *
   * @param xrpPayment an {@link XrpPayment} containing details about the received payment.
   * @return The Invoice that was updated as a result of an XRP payment, or empty if the payment was not meant for
   *          an invoice.
   */
  @RequestMapping(
    path = OpenPaymentsPathConstants.SLASH_INVOICES + "/payment/xrp",
    method = RequestMethod.POST,
    produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public Optional<Invoice> onXrpPayment(@RequestBody XrpPayment xrpPayment) {
    return invoiceService.onPayment(xrpPayment);
  }

  /**
   * Endpoint to notify the Open Payments Server that ILP payment has been received.
   *
   * The Open Payments Server will then decode the connection tag of the destination address and determine
   * if the payment was meant for an Invoice.
   *
   * @param streamPayment a {@link StreamPayment} containing details about the received ILP payment.
   * @return The Invoice that was updated as a result of an ILP payment, or empty if the payment was not meant for
   *          an invoice.
   */
  @RequestMapping(
    path = OpenPaymentsPathConstants.SLASH_INVOICES + "/payment/ilp",
    method = RequestMethod.POST,
    produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public Optional<Invoice> onIlpPayment(@RequestBody StreamPayment streamPayment) {
    return invoiceService.onPayment(streamPayment);
  }

  /**
   * Generate payment details for any supported payment rail.
   *
   * For ILP payments, this logic will largely be the same as an SPSP server's setup logic,
   * except that the connection tag on the destination address will be in the form:
   * (randomToken) + '~' + (invoiceId in Base64).
   *
   * For XRP payments, this will return an XRP address and the invoiceId encoded in Base64 as a destination tag.
   *
   * XRP payment details can be requested by using the "application/json+xrp-opa" MIME type in the Accept header.
   *
   * @param invoiceId The {@link InvoiceId} of the {@link Invoice} this payment is being set up to pay.
   * @return The payment details necessary to pay an invoice.
   */
  /*@RequestMapping(
    path = OpenPaymentsPathConstants.SLASH_INVOICES + OpenPaymentsPathConstants.SLASH_INVOICE_ID,
    method = RequestMethod.OPTIONS,
    produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public @ResponseBody ResponseEntity getPaymentDetails(
    @PathVariable(name = OpenPaymentsPathConstants.INVOICE_ID) InvoiceId invoiceId
  ) {
    final HttpHeaders headers = new HttpHeaders();
    headers.setLocation(getInvoiceLocation(invoiceId));

    // Get the existing invoice
    final Invoice invoice = invoiceService.getInvoiceById(invoiceId);

    // This invoice is not "ours", so go get the payment details from the receiver's OPS
    if (!isOurs(invoice)) {
      return this.getReceiverPaymentDetails(invoice);
    }

    // XRP payment details are not supported yet, so just return a bad request status
    if (invoice.paymentNetwork().equals(PaymentNetwork.XRPL)) {
      try {
        // Get XRP address from payment pointer and invoiceId
        final String destinationAddress = xrpPaymentDetailsService.getAddressFromInvoiceSubject(invoice.subject());

        XrpPaymentDetails xrpPaymentDetails = XrpPaymentDetails.builder()
          .address(destinationAddress)
          .invoiceIdHash(invoice.paymentId())
          .build();

        return new ResponseEntity(xrpPaymentDetails, headers, HttpStatus.OK);
      } catch (RuntimeException e) {
        throw new InvoicePaymentDetailsProblem(e.getCause().getMessage(), invoiceId);
      }
    } else {
      // Otherwise get ILP payment details

      // Get ILP Address Prefix from payment pointer and invoiceId
      final String destinationAddress = ilpPaymentDetailsService.getAddressFromInvoiceSubject(invoice.subject());
      // Get shared secret and address with connection tag
      final StreamConnectionDetails streamConnectionDetails =
        streamConnectionGenerator.generateConnectionDetails(serverSecretSupplier, InterledgerAddress.of(destinationAddress));



      // Append the encoded invoiceId to the connection tag and return
      final StreamConnectionDetails streamDetailsWithInvoiceIdTag =
        StreamConnectionDetails.builder()
          .from(streamConnectionDetails)
          .destinationAddress(InterledgerAddress
            .of(streamConnectionDetails.destinationAddress().getValue() + "~" + invoice.paymentId()))
          .build();

      return new ResponseEntity(streamDetailsWithInvoiceIdTag, headers, HttpStatus.OK);
    }
  }

  protected ResponseEntity getReceiverPaymentDetails(Invoice invoice) {
    HttpUrl receiverUrl;
    receiverUrl = resolveSubjectToHttpUrl(invoice.subject());

    // Get the invoices endpoint on the receiver
    // TODO: use invoice location instead of metadata
    OpenPaymentsMetadata metadata = openPaymentsClient.getMetadata(receiverUrl.uri());

    // Get and return the correct payment details.
    if (invoice.paymentNetwork().equals(PaymentNetwork.XRPL)) {
      XrpPaymentDetails xrpInvoicePaymentDetails =
        openPaymentsClient.getXrpInvoicePaymentDetails(metadata.invoicesEndpoint().uri(), invoice.id().value());
      return new ResponseEntity(xrpInvoicePaymentDetails, HttpStatus.OK);
    } else {
      StreamConnectionDetails ilpInvoicePaymentDetails =
        openPaymentsClient.getIlpInvoicePaymentDetails(metadata.invoicesEndpoint().uri(), invoice.id().value());

      return new ResponseEntity(ilpInvoicePaymentDetails, HttpStatus.OK);
    }
  }*/

  // TODO: Is this how we want to determine if an invoice is ours or not?
  private boolean isOurs(Invoice invoice) {
    HttpUrl subjectUrl = resolveSubjectToHttpUrl(invoice.subject());

    HttpUrl accountIdUrl = resolveSubjectToHttpUrl(invoice.accountId().orElse(""));
    if (accountIdUrl != null) {
      return subjectUrl.host().equals(accountIdUrl.host());
    }

    return false;
  }

  private HttpUrl resolveSubjectToHttpUrl(String subject) {
    HttpUrl receiverUrl;// Try to parse invoice subject as a Payment Pointer, otherwise assume it's a PayID and parse that.
    try {
      PaymentPointer paymentPointer = PaymentPointer.of(subject);
      receiverUrl = paymentPointerResolver.resolveHttpUrl(paymentPointer);
    } catch (IllegalArgumentException e) {
      try {
        String subjectHost = subject.substring(subject.lastIndexOf("$") + 1);
        receiverUrl = new HttpUrl.Builder()
          .scheme("https")
          .host(subjectHost)
          .build();
      } catch (IndexOutOfBoundsException oobe) {
        return null;
      }
    }
    return receiverUrl;
  }

  private URI getInvoiceLocation(InvoiceId invoiceId) {
    return openPaymentsSettingsSupplier
      .get()
      .metadata()
      .invoicesEndpoint()
      .newBuilder()
      .addPathSegment(invoiceId.toString())
      .build()
      .uri();
  }
}
