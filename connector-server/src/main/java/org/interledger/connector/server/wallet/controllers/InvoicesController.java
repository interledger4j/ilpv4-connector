package org.interledger.connector.server.wallet.controllers;

import static org.interledger.connector.core.ConfigConstants.ENABLED_PROTOCOLS;
import static org.interledger.connector.core.ConfigConstants.SPSP_ENABLED;
import static org.interledger.connector.core.ConfigConstants.TRUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.interledger.connector.opa.InvoiceService;
import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.InvoiceId;
import org.interledger.connector.opa.model.OpenPaymentsMediaType;
import org.interledger.connector.opa.model.OpenPaymentsSettings;
import org.interledger.connector.opa.model.PaymentDetails;
import org.interledger.connector.opa.model.PaymentResponse;
import org.interledger.connector.opa.model.XrpPayment;
import org.interledger.connector.payments.StreamPayment;
import org.interledger.connector.settings.properties.OpenPaymentsPathConstants;
import org.interledger.spsp.server.grpc.SendPaymentResponse;

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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
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

  public InvoicesController(
    final InvoiceService invoiceService,
    final Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier
  ) {
    this.invoiceService = Objects.requireNonNull(invoiceService);
    this.openPaymentsSettingsSupplier = Objects.requireNonNull(openPaymentsSettingsSupplier);
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
  public @ResponseBody ResponseEntity<Invoice> createInvoice(
    @PathVariable String accountId,
    @RequestBody Invoice invoice
  ) {
    Invoice createdInvoice = invoiceService.createInvoice(invoice);

    final HttpHeaders headers = new HttpHeaders();
    headers.setLocation(getInvoiceLocation(invoice.id()));
    return new ResponseEntity(createdInvoice, headers, HttpStatus.CREATED);
  }

  /**
   * Get an existing {@link Invoice}. If the invoice URL that we have for this invoiceID has a different host
   * than this server, this will get the invoice from that location and update the local copy.
   *
   * @param invoiceId The {@link InvoiceId} of the {@link Invoice} being retrieved.
   * @return An existing {@link Invoice}
   */
  @RequestMapping(
    path = OpenPaymentsPathConstants.SLASH_ACCOUNT_ID + OpenPaymentsPathConstants.SLASH_INVOICES + OpenPaymentsPathConstants.SLASH_INVOICE_ID,
    method = RequestMethod.GET,
    produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public @ResponseBody Invoice getInvoice(
    @PathVariable(name = OpenPaymentsPathConstants.INVOICE_ID) InvoiceId invoiceId
  ) {
    return invoiceService.getInvoiceById(invoiceId);
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
  @RequestMapping(
    path = OpenPaymentsPathConstants.SLASH_ACCOUNT_ID + OpenPaymentsPathConstants.SLASH_INVOICES + OpenPaymentsPathConstants.SLASH_INVOICE_ID,
    method = RequestMethod.GET,
    produces = {OpenPaymentsMediaType.APPLICATION_CONNECTION_JSON_VALUE, APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public @ResponseBody ResponseEntity getPaymentDetails(
    @PathVariable(name = OpenPaymentsPathConstants.ACCOUNT_ID) String accountId,
    @PathVariable(name = OpenPaymentsPathConstants.INVOICE_ID) InvoiceId invoiceId
  ) {
    final HttpHeaders headers = new HttpHeaders();
    headers.setLocation(getInvoiceLocation(invoiceId));

    final PaymentDetails paymentDetails = invoiceService.getPaymentDetails(invoiceId);

    return new ResponseEntity(paymentDetails, headers, HttpStatus.OK);
  }

  /**
   *
   * @param accountId
   * @param invoiceId
   * @return
   */
  @RequestMapping(
    path = OpenPaymentsPathConstants.SLASH_ACCOUNT_ID + OpenPaymentsPathConstants.SLASH_INVOICES + OpenPaymentsPathConstants.SLASH_INVOICE_ID + OpenPaymentsPathConstants.SLASH_PAY,
    method = RequestMethod.POST,
    produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public PaymentResponse payInvoice(
    @PathVariable(name = OpenPaymentsPathConstants.ACCOUNT_ID) String accountId,
    @PathVariable(name = OpenPaymentsPathConstants.INVOICE_ID) InvoiceId invoiceId
  ) {
    return null;
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
