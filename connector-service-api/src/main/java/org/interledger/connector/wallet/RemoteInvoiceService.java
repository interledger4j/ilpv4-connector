package org.interledger.connector.wallet;

import org.interledger.connector.opa.model.Invoice;

import okhttp3.HttpUrl;

public interface RemoteInvoiceService {

  Invoice getInvoice(HttpUrl invoiceUrl);
}
