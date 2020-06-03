package org.interledger.connector.server.wallet.controllers;

import static org.interledger.connector.core.ConfigConstants.ENABLED_PROTOCOLS;
import static org.interledger.connector.core.ConfigConstants.OPEN_PAYMENTS_ENABLED;
import static org.interledger.connector.core.ConfigConstants.TRUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.opa.InvoiceService;
import org.interledger.connector.opa.model.NewInvoice;
import org.interledger.connector.opa.model.IlpPaymentDetails;
import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.InvoiceId;
import org.interledger.connector.opa.model.OpenPaymentsMediaType;
import org.interledger.connector.opa.model.OpenPaymentsSettings;
import org.interledger.connector.opa.model.PayInvoiceRequest;
import org.interledger.connector.payments.StreamPayment;
import org.interledger.connector.opa.model.OpenPaymentsPathConstants;

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
   * Create an {@link Invoice} on the Open Payments server.
   *
   * @param newInvoice An {@link Invoice} to create on the Open Payments server.
   * @return A 201 Created if successful, and the fully populated {@link Invoice} which was stored.
   */
  @RequestMapping(
    path = OpenPaymentsPathConstants.INVOICES_BASE,
    method = RequestMethod.POST,
    produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public @ResponseBody ResponseEntity<Invoice> createInvoice(
    @PathVariable(name = OpenPaymentsPathConstants.ACCOUNT_ID) AccountId accountId,
    @RequestBody NewInvoice newInvoice
  ) {
    Invoice createdInvoice = ilpInvoiceService.createInvoice(newInvoice, accountId);

    final HttpHeaders headers = new HttpHeaders();
    headers.setLocation(getInvoiceLocation(createdInvoice.id()));
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
   * Get either an {@link Invoice} or {@link IlpPaymentDetails} for an {@link Invoice}.
   *
   * Note that retrieval of payment details is only supported for Interledger Payments.
   *
   * @param accountId The {@link AccountId} of the {@link Invoice} owner.
   * @param invoiceId The {@link InvoiceId} of the {@link Invoice}.
   * @param acceptHeader "application/json" to get an {@link Invoice}, or "application/ilp-stream+json" to get {@link IlpPaymentDetails}.
   * @return A {@link ResponseEntity} containing the {@link Invoice} or the {@link IlpPaymentDetails}.
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
    Invoice invoice = ilpInvoiceService.getInvoice(invoiceId, accountId);
    return new ResponseEntity(invoice, headers, HttpStatus.OK);
  }

  /**
   * Make a payment towards an {@link Invoice}.
   *
   * Note that this endpoint should only exist for custodial wallets which can make payments on behalf of a sender.
   * Non-custodial wallets should instead get payment details for an {@link Invoice} and execute the payment
   * from the client.
   *
   * Also note that currently, this endpoint only supports paying invoices over ILP.
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
  // FIXME: this is weird... I think a client would expect to get a Payment back, which it can then query
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


  // POST /accounts/foo/invoices/123 w/ NewInvoice (Create invoice)
  // POST /accounts/foo/invoices/sync?name={remoteInvoiceUrl} (Sync invoice)
  // GET /accounts/foo/invoices/123 (Get invoice)
  // GET /accounts/foo/invoices/123 Accept=application/ilp-stream+json (Get payment details)
  // POST /accounts/foo/invoices/123/pay (Pay and invoice over ilp)
}
