package org.interledger.connector.payments;

import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerAddress;

import java.math.BigInteger;
import java.util.Optional;

/**
 * Service for initiating send payments for accounts on this connector.
 */
public interface SendPaymentService {

  StreamPayment sendMoney(SendPaymentRequest request);

  StreamPayment createPlaceholderPayment(
    AccountId accountId,
    StreamPaymentType type,
    InterledgerAddress destinationAddress,
    Optional<String> correlationId,
    Optional<BigInteger> expectedAmount
  );
}
