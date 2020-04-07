package org.interledger.connector.opay.controllers;

import static org.interledger.connector.opay.config.OpenPaymentsConfig.OPEN_PAYMENTS;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.interledger.connector.opay.InvoiceId;
import org.interledger.connector.opay.controllers.constants.PathConstants;
import org.interledger.connector.opay.model.Invoice;
import org.interledger.connector.opay.model.OpenPaymentsMetadata;
import org.interledger.connector.opay.service.InvoiceService;
import org.interledger.core.InterledgerAddress;
import org.interledger.spsp.StreamConnectionDetails;
import org.interledger.stream.receiver.ServerSecretSupplier;
import org.interledger.stream.receiver.StreamConnectionGenerator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import java.util.Base64;
import java.util.Objects;
import java.util.function.Supplier;

@RestController
public class InvoicesController {

  private final StreamConnectionGenerator streamConnectionGenerator;
  private InvoiceService invoiceService;
  private final Supplier<OpenPaymentsMetadata> openPaymentsMetadataSupplier;
  private ServerSecretSupplier serverSecretSupplier;

  @Autowired
  ObjectMapper objectMapper;

  public InvoicesController(
    final InvoiceService invoiceService,
    @Qualifier(OPEN_PAYMENTS) final StreamConnectionGenerator streamConnectionGenerator,
    final Supplier<OpenPaymentsMetadata> openPaymentsMetadataSupplier,
    final ServerSecretSupplier serverSecretSupplier
  ) {
    this.invoiceService = Objects.requireNonNull(invoiceService);
    this.streamConnectionGenerator = Objects.requireNonNull(streamConnectionGenerator);
    this.openPaymentsMetadataSupplier = Objects.requireNonNull(openPaymentsMetadataSupplier);
    this.serverSecretSupplier = Objects.requireNonNull(serverSecretSupplier);
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
    path = PathConstants.SLASH_INVOICE,
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
    path = PathConstants.SLASH_INVOICE + "/{invoiceId}",
    method = RequestMethod.GET,
    produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public @ResponseBody Invoice getInvoice(@PathVariable InvoiceId invoiceId) {
    return invoiceService.getInvoiceById(invoiceId);
  }

  /**
   * Generate payment details for a STREAM payment, including a destination address and a shared secret.
   *
   * This logic will largely be the same as an SPSP server's setup logic, except that the connection tag
   * on the destination address will be in the form: (randomToken) + '~' + (invoiceId in Base64).
   *
   * @param invoiceId The {@link InvoiceId} of the {@link Invoice} this payment is being set up to pay.
   * @return The {@link StreamConnectionDetails} needed to send a STREAM payment in the context of the {@link Invoice}
   */
  @RequestMapping(
    path = PathConstants.SLASH_INVOICE + "/{invoiceId}",
    method = RequestMethod.OPTIONS,
    produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public @ResponseBody ResponseEntity<StreamConnectionDetails> getPaymentDetails(@PathVariable InvoiceId invoiceId) {
    // Get the existing invoice
    final Invoice invoice = invoiceService.getInvoiceById(invoiceId);

    // Get ILP Address Prefix from payment pointer and invoiceId
    final String destinationAddress = invoiceService.getAddressFromInvoice(invoice);

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

  private URI getInvoiceLocation(InvoiceId invoiceId) {
    return openPaymentsMetadataSupplier
      .get()
      .invoicesEndpoint()
      .newBuilder()
      .addPathSegment(invoiceId.toString())
      .build()
      .uri();
  }
}
