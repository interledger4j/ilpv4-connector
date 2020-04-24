package org.interledger.connector.opa;

import org.interledger.connector.opa.model.PayIdOpaPaymentRequest;
import org.interledger.connector.opa.model.PaymentResponse;

public interface OpaPaymentService {

  PaymentResponse sendOpaPayment(PayIdOpaPaymentRequest payIdOpaPaymentRequest, String accountId, String bearerToken) throws Exception;

}
