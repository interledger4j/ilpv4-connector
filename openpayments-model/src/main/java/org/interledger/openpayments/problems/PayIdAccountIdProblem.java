package org.interledger.openpayments.problems;

import org.zalando.problem.StatusType;

import java.net.URI;

/**
 * A root exception for all exceptions relating to invoices.
 */
public class PayIdAccountIdProblem extends AbstractOpenPaymentsProblem {

  public static final String ACCOUNT_ID_PATH = "/payIdAccountId";

  /**
   * The invoice id of the invoice that threw this exception.
   */
  public PayIdAccountIdProblem(URI type, String title, StatusType status) {
    super(type, title, status);
  }

  public PayIdAccountIdProblem(URI type, String title, StatusType status, String detail) {
    super(type, title, status, detail);
  }

}
