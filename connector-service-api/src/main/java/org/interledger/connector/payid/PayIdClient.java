package org.interledger.connector.payid;

import org.interledger.openpayments.PayId;
import org.interledger.openpayments.PaymentNetwork;

import java.net.URI;

public interface PayIdClient {

  PayIdResponse getPayId(URI baseUri,
                         String account,
                         PaymentNetwork paymentNetwork,
                         String environment);

  default PayIdResponse getPayId(PayId payId, PaymentNetwork paymentNetwork, String environment) {
    return getPayId(payId.baseUrl().uri(), payId.account(), paymentNetwork, environment);
  }

}