package org.interleger.openpayments.client;

import org.interledger.openpayments.Mandate;

public interface WebhookClient {

  void sendMandateStatusChange(Mandate mandate);

}
