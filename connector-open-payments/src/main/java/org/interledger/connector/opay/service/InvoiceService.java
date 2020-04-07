package org.interledger.connector.opay.service;

import org.interledger.connector.opay.InvoiceId;
import org.interledger.connector.opay.model.Invoice;

public interface InvoiceService {

  Invoice getInvoiceById(final InvoiceId invoiceId);

  Invoice createInvoice(final Invoice invoice);

  Invoice updateInvoice(final Invoice invoice);

  String getAddressFromInvoice(final Invoice invoice);
}
