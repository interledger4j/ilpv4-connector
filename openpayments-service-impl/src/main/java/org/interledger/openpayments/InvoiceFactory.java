package org.interledger.openpayments;

import okhttp3.HttpUrl;

/**
 * Factory class for creating {@link Invoice}s.
 */
public class InvoiceFactory {

  /**
   * Populate the URL of an invoice.  This MUST be called before creating an {@link Invoice} in the database.
   *
   * Note that this should only be called by the invoice owner's OPS.
   *
   * @param newInvoice An {@link Invoice} without a URL.
   * @return An {@link Invoice} with a URL.
   */
  public Invoice construct(NewInvoice newInvoice) {

    HttpUrl ownerAccountUrl = newInvoice.ownerAccountUrl();

    String accountId = ownerAccountUrl.pathSegments().get(ownerAccountUrl.pathSegments().size() - 1);

    return Invoice.builder()
      .ownerAccountUrl(ownerAccountUrl)
      .accountId(PayIdAccountId.of(accountId))
      .amount(newInvoice.amount())
      .assetCode(newInvoice.assetCode())
      .assetScale(newInvoice.assetScale())
      .subject(newInvoice.subject())
      .description(newInvoice.description())
      .build();
  }

}
