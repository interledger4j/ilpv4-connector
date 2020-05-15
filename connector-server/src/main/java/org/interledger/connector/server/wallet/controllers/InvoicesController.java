package org.interledger.connector.server.wallet.controllers;

import static org.interledger.connector.core.ConfigConstants.ENABLED_PROTOCOLS;
import static org.interledger.connector.core.ConfigConstants.SPSP_ENABLED;
import static org.interledger.connector.core.ConfigConstants.TRUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.opa.InvoiceService;
import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.InvoiceId;
import org.interledger.connector.opa.model.OpenPaymentsMediaType;
import org.interledger.connector.opa.model.OpenPaymentsSettings;
import org.interledger.connector.opa.model.PaymentDetails;
import org.interledger.connector.opa.model.XrpPayment;
import org.interledger.connector.payments.StreamPayment;
import org.interledger.connector.server.spring.controllers.PathConstants;
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
    path = OpenPaymentsPathConstants.INVOICES_BASE,
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

  @RequestMapping(
    path = OpenPaymentsPathConstants.SYNC_INVOICE,
    method = RequestMethod.POST,
    produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public @ResponseBody Invoice getOrSyncInvoice(
    @RequestParam("name") String invoiceUrl
  ) throws UnsupportedEncodingException {
    String decodedInvoiceUrl = URLDecoder.decode(invoiceUrl, StandardCharsets.UTF_8.toString());
    return invoiceService.getOrSyncInvoice(HttpUrl.get(decodedInvoiceUrl));
  }

  @RequestMapping(
    path = OpenPaymentsPathConstants.INVOICES_WITH_ID,
    method = RequestMethod.GET,
    produces = {OpenPaymentsMediaType.APPLICATION_CONNECTION_JSON_VALUE, APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public @ResponseBody ResponseEntity getInvoiceDetails(
    @PathVariable(name = OpenPaymentsPathConstants.INVOICE_ID) InvoiceId invoiceId,
    @RequestHeader("Accept") String acceptHeader
  ) {
    final HttpHeaders headers = new HttpHeaders();
    headers.setLocation(getInvoiceLocation(invoiceId));

    if (acceptHeader.equals(OpenPaymentsMediaType.APPLICATION_CONNECTION_JSON_VALUE)) {
      return new ResponseEntity(getPaymentDetails(invoiceId), headers, HttpStatus.OK);
    } else {
      return new ResponseEntity(getInvoice(invoiceId), headers, HttpStatus.OK);
    }
  }

  /**
   * Get an existing {@link Invoice}. If the invoice URL that we have for this invoiceID has a different host
   * than this server, this will get the invoice from that location and update the local copy.
   *
   * @param invoiceId The {@link InvoiceId} of the {@link Invoice} being retrieved.
   * @return An existing {@link Invoice}
   */
  public Invoice getInvoice(InvoiceId invoiceId) {
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
  private PaymentDetails getPaymentDetails(InvoiceId invoiceId) {
    return invoiceService.getPaymentDetails(invoiceId);
  }

  /**
   *
   * @param accountId
   * @param invoiceId
   * @return
   */
  @RequestMapping(
    path = OpenPaymentsPathConstants.PAY_INVOICE,
    method = RequestMethod.POST,
    produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public StreamPayment payInvoice(
    @PathVariable(name = OpenPaymentsPathConstants.ACCOUNT_ID) String accountId,
    @PathVariable(name = OpenPaymentsPathConstants.INVOICE_ID) InvoiceId invoiceId
  ) {
    return invoiceService.payInvoice(invoiceId, AccountId.of(accountId));
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
