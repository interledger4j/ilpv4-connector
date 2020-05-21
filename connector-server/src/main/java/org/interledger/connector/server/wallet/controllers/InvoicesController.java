package org.interledger.connector.server.wallet.controllers;

import static org.interledger.connector.core.ConfigConstants.ENABLED_PROTOCOLS;
import static org.interledger.connector.core.ConfigConstants.OPEN_PAYMENTS_ENABLED;
import static org.interledger.connector.core.ConfigConstants.TRUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.opa.InvoiceService;
import org.interledger.connector.opa.model.IlpPaymentDetails;
import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.InvoiceId;
import org.interledger.connector.opa.model.OpenPaymentsMediaType;
import org.interledger.connector.opa.model.OpenPaymentsSettings;
import org.interledger.connector.opa.model.PayInvoiceRequest;
import org.interledger.connector.payments.StreamPayment;
import org.interledger.connector.settings.properties.OpenPaymentsPathConstants;

import okhttp3.HttpUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.spring.common.MediaTypes;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

@RestController
@ConditionalOnProperty(prefix = ENABLED_PROTOCOLS, name = OPEN_PAYMENTS_ENABLED, havingValue = TRUE)
public class InvoicesController {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private InvoiceService<StreamPayment, IlpPaymentDetails> ilpInvoiceService;
  private final Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier;

  public InvoicesController(
    final InvoiceService<StreamPayment, IlpPaymentDetails> ilpInvoiceService,
    final Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier
  ) {
    this.ilpInvoiceService = Objects.requireNonNull(ilpInvoiceService);
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
    path = OpenPaymentsPathConstants.INVOICES_BASE,
    method = RequestMethod.POST,
    produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public @ResponseBody ResponseEntity<Invoice> createInvoice(
    @PathVariable AccountId accountId,
    @RequestBody Invoice invoice
  ) {
    Invoice createdInvoice = ilpInvoiceService.createInvoice(invoice, accountId);

    final HttpHeaders headers = new HttpHeaders();
    headers.setLocation(getInvoiceLocation(invoice.id()));
    return new ResponseEntity(createdInvoice, headers, HttpStatus.CREATED);
  }

  /**
   * Get and save the latest state of the invoice located at a remote OPS at {@code invoiceUrl},
   * if the invoice with that location does not already exist on this Open Payments Server.
   *
   * Can only be called once per invoice.  A 409 HTTP status code response from this endpoint should indicate to the
   * client that the invoice has already been synced from the remote Open Payments Server.
   *
   * @param invoiceUrl The unique URL of the {@link Invoice}.
   * @return The synced {@link Invoice}.
   * @throws UnsupportedEncodingException if {@code invoiceUrl} can not be decoded as a URL.
   */
  @RequestMapping(
    path = OpenPaymentsPathConstants.SYNC_INVOICE,
    method = RequestMethod.POST,
    produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public @ResponseBody Invoice syncInvoice(
    @PathVariable(name = OpenPaymentsPathConstants.ACCOUNT_ID) AccountId accountId,
    @RequestParam("name") String invoiceUrl
  ) throws UnsupportedEncodingException {
    String decodedInvoiceUrl = URLDecoder.decode(invoiceUrl, StandardCharsets.UTF_8.toString());
    return ilpInvoiceService.syncInvoice(HttpUrl.get(decodedInvoiceUrl), accountId);
  }

  /**
   *
   */
  @RequestMapping(
    path = OpenPaymentsPathConstants.INVOICES_WITH_ID,
    method = RequestMethod.GET,
    produces = {OpenPaymentsMediaType.APPLICATION_CONNECTION_JSON_VALUE, APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public @ResponseBody ResponseEntity getInvoiceDetails(
    @PathVariable(name = OpenPaymentsPathConstants.ACCOUNT_ID) AccountId accountId,
    @PathVariable(name = OpenPaymentsPathConstants.INVOICE_ID) InvoiceId invoiceId,
    @RequestHeader("Accept") String acceptHeader
  ) {
    final HttpHeaders headers = new HttpHeaders();
    headers.setLocation(getInvoiceLocation(invoiceId));

    if (acceptHeader.equals(OpenPaymentsMediaType.APPLICATION_CONNECTION_JSON_VALUE)) {
      IlpPaymentDetails paymentDetails = ilpInvoiceService.getPaymentDetails(invoiceId, accountId);
      return new ResponseEntity(paymentDetails, headers, HttpStatus.OK);
    }
    Invoice invoice = ilpInvoiceService.getInvoiceById(invoiceId, accountId);
    return new ResponseEntity(invoice, headers, HttpStatus.OK);
  }

  /**
   * Make a payment towards an {@link Invoice}.
   *
   * Note that this endpoint should only exist for custodial wallets which can make payments on behalf of a sender.
   * Non-custodial wallets should instead get {@link IlpPaymentDetails} for an {@link Invoice} and execute the payment
   * from the client.
   *
   * @param accountId The {@link AccountId} of the sender.
   * @param invoiceId The {@link InvoiceId} of the {@link Invoice} to pay.
   * @param payInvoiceRequest Optional request body containing the amount to pay on the {@link Invoice}.
   * @return The result of the payment.
   */
  @RequestMapping(
    path = OpenPaymentsPathConstants.PAY_INVOICE,
    method = RequestMethod.POST,
    produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  // TODO: Create a generic type for payments instead of coupling to ILP. (Wait for David's changes)
  public StreamPayment payInvoice(
    @PathVariable(name = OpenPaymentsPathConstants.ACCOUNT_ID) AccountId accountId,
    @PathVariable(name = OpenPaymentsPathConstants.INVOICE_ID) InvoiceId invoiceId,
    @RequestBody Optional<PayInvoiceRequest> payInvoiceRequest
  ) {
    return ilpInvoiceService.payInvoice(invoiceId, accountId, payInvoiceRequest);
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
