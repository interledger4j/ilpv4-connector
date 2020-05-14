package org.interledger.connector.payments;

import org.interledger.connector.accounts.AccountId;
import org.interledger.spsp.StreamConnectionDetails;

import com.google.common.primitives.UnsignedLong;

/**
 * Service for initiating send payments for accounts on this connector.
 */
public interface SendPaymentService {

  StreamPayment sendMoney(SendPaymentRequest request);

  StreamPayment sendMoney(AccountId senderAccountId, StreamConnectionDetails receiverDetails, UnsignedLong amount);

}
