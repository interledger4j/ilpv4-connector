package org.interledger.connector.opa.service;


import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.InvoiceId;

public interface InvoiceService {

  Invoice getInvoiceById(final InvoiceId invoiceId);

  Invoice createInvoice(final Invoice invoice);

  Invoice updateInvoice(final Invoice invoice);

  String getAddressFromInvoice(final Invoice invoice);
}
