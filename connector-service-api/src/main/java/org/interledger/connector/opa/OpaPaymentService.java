package org.interledger.connector.opa;

import org.interledger.connector.opa.model.PaymentRequest;
import org.interledger.connector.opa.model.PaymentResponse;

import java.util.concurrent.ExecutionException;

public interface OpaPaymentService {

  PaymentResponse sendOpaPayment(PaymentRequest paymentRequest, String accountId, String bearerToken) throws ExecutionException, InterruptedException;

}
