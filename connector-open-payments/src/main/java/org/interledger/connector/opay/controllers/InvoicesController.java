package org.interledger.connector.opay.controllers;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.interledger.connector.opay.InvoiceId;
import org.interledger.connector.opay.controllers.constants.PathConstants;
import org.interledger.connector.opay.model.Invoice;
import org.interledger.connector.opay.model.OpenPaymentsMetadata;
import org.interledger.connector.opay.service.InvoiceService;
import org.interledger.core.InterledgerAddress;
import org.interledger.spsp.StreamConnectionDetails;
import org.interledger.stream.receiver.StreamReceiver;

import org.springframework.http.HttpEntity;
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

import java.util.Base64;
import java.util.Objects;
import java.util.function.Supplier;

@RestController
public class InvoicesController {

  private InvoiceService invoiceService;
  private StreamReceiver streamReceiver;
  private final Supplier<OpenPaymentsMetadata> openPaymentsMetadataSupplier;

  public InvoicesController(
    final InvoiceService invoiceService,
    final StreamReceiver streamReceiver,
    final Supplier<OpenPaymentsMetadata> openPaymentsMetadataSupplier
    ) {
    this.invoiceService = Objects.requireNonNull(invoiceService);
    this.streamReceiver = Objects.requireNonNull(streamReceiver);
    this.openPaymentsMetadataSupplier = Objects.requireNonNull(openPaymentsMetadataSupplier);
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
  public @ResponseBody HttpEntity<Invoice> createInvoice(@RequestBody Invoice invoice) {
    return null;
  }

  /**
   * Get an existing {@link Invoice}.
   *
   * @param invoiceId The {@link InvoiceId} of the {@link Invoice} being retrieved.
   * @return An existing {@link Invoice}
   */
  @RequestMapping(
    path = PathConstants.SLASH_INVOICE + "/id",
    method = RequestMethod.GET,
    produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public @ResponseBody Invoice getInvoice(@PathVariable InvoiceId invoiceId) {
    return null;
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
    path = PathConstants.SLASH_INVOICE + "/id",
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
      streamReceiver.setupStream(InterledgerAddress.of(destinationAddress));

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
    headers.setLocation(openPaymentsMetadataSupplier
      .get()
      .invoicesEndpoint()
      .newBuilder()
      .addPathSegment(invoiceId.toString())
      .build()
      .uri());

    return new ResponseEntity(streamDetailsWithInvoiceIdTag, headers, HttpStatus.OK);
  }
}
