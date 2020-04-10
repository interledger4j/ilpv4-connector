package org.interledger.connector.server.wallet.controllers;

import static org.interledger.connector.core.ConfigConstants.ENABLED_PROTOCOLS;
import static org.interledger.connector.core.ConfigConstants.SPSP_ENABLED;
import static org.interledger.connector.core.ConfigConstants.TRUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.interledger.connector.opa.InvoiceService;
import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.InvoiceId;
import org.interledger.connector.opa.model.OpenPaymentsSettings;
import org.interledger.connector.settings.properties.OpenPaymentsPathConstants;
import org.interledger.core.InterledgerAddress;
import org.interledger.spsp.StreamConnectionDetails;
import org.interledger.stream.receiver.ServerSecretSupplier;
import org.interledger.stream.receiver.StreamConnectionGenerator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.spring.common.MediaTypes;

import java.net.URI;
import java.util.Base64;
import java.util.Objects;
import java.util.function.Supplier;

@RestController
// TODO make an enabled property for open payments?
@ConditionalOnProperty(prefix = ENABLED_PROTOCOLS, name = SPSP_ENABLED, havingValue = TRUE)
public class InvoicesController {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private static final String APPLICATION_JSON_XRP_OPA_VALUE = "application/json+xrp-opa";
  private static final MediaType APPLICATION_JSON_XRP_OPA = MediaType.valueOf(APPLICATION_JSON_XRP_OPA_VALUE);
  private InvoiceService invoiceService;
  private final Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier;
  private final ServerSecretSupplier serverSecretSupplier;
  private final StreamConnectionGenerator streamConnectionGenerator;

  @Autowired
  ObjectMapper objectMapper;

  public InvoicesController(
    final InvoiceService invoiceService,
    final Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier,
    final ServerSecretSupplier serverSecretSupplier,
    final StreamConnectionGenerator streamConnectionGenerator
    ) {
    this.invoiceService = Objects.requireNonNull(invoiceService);
    this.streamConnectionGenerator = Objects.requireNonNull(streamConnectionGenerator);
    this.openPaymentsSettingsSupplier = Objects.requireNonNull(openPaymentsSettingsSupplier);
    this.serverSecretSupplier = Objects.requireNonNull(serverSecretSupplier);
    logger.info("Invoices controller starting the FUCK up");
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
    path = OpenPaymentsPathConstants.SLASH_INVOICE,
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
   * Get an existing {@link Invoice}.
   *
   * @param invoiceId The {@link InvoiceId} of the {@link Invoice} being retrieved.
   * @return An existing {@link Invoice}
   */
  @RequestMapping(
    path = OpenPaymentsPathConstants.SLASH_INVOICE + "/{invoiceId}",
    method = RequestMethod.GET,
    produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public @ResponseBody Invoice getInvoice(@PathVariable InvoiceId invoiceId) {
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
    path = OpenPaymentsPathConstants.SLASH_INVOICE + "/{invoiceId}",
    method = RequestMethod.OPTIONS,
    produces = {APPLICATION_JSON_XRP_OPA_VALUE, APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public @ResponseBody ResponseEntity getPaymentDetails(
    @RequestHeader("Accept") String acceptHeaderValue,
    @PathVariable InvoiceId invoiceId
  ) {
    // XRP payment details are not supported yet, so just return a bad request status
    if (acceptHeaderValue.equals(APPLICATION_JSON_XRP_OPA_VALUE)) {
      return new ResponseEntity(HttpStatus.BAD_REQUEST);
    } else {
      // Otherwise get ILP payment details

      // Get the existing invoice
      final Invoice invoice = invoiceService.getInvoiceById(invoiceId);

      // Get ILP Address Prefix from payment pointer and invoiceId
      final String destinationAddress = invoiceService.getAddressFromInvoiceSubject(invoice.subject());


      // Get shared secret and address with connection tag
      final StreamConnectionDetails streamConnectionDetails =
        streamConnectionGenerator.generateConnectionDetails(serverSecretSupplier, InterledgerAddress.of(destinationAddress));

      // Base64 encode the invoiceId to add to the connection tag
      final byte[] invoiceIdBytes = invoiceId.value().toString().getBytes();
      final String encodedInvoiceId = Base64.getUrlEncoder().withoutPadding().encodeToString(invoiceIdBytes);

      // Append the encoded invoiceId to the connection tag and return
      final StreamConnectionDetails streamDetailsWithInvoiceIdTag =
        StreamConnectionDetails.builder()
          .from(streamConnectionDetails)
          .destinationAddress(streamConnectionDetails.destinationAddress().with("~" + encodedInvoiceId))
          .build();

      final HttpHeaders headers = new HttpHeaders();
      headers.setLocation(getInvoiceLocation(invoiceId));

      return new ResponseEntity(streamDetailsWithInvoiceIdTag, headers, HttpStatus.OK);
    }
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
