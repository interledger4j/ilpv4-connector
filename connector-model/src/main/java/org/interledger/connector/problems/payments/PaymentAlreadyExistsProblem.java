package org.interledger.connector.problems.payments;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.core.problems.AbstractConnectorProblem;

import org.zalando.problem.Status;

import java.net.URI;

/**
 * Exception to indicate that the request to send payment conflicts with an already existing steam payment with
 * values that are different than requested.
 */
public class PaymentAlreadyExistsProblem extends AbstractConnectorProblem {

  public PaymentAlreadyExistsProblem(AccountId accountId, String streamPaymentId) {
    super(
      URI.create(TYPE_PREFIX + "/accounts/" + accountId + "/payments/" + streamPaymentId),
      "Payment already exists but with different parameters than requested",
      Status.CONFLICT
    );
  }
}
