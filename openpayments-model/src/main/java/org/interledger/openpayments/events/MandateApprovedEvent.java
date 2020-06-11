package org.interledger.openpayments.events;

import org.interledger.connector.accounts.AccountId;
import org.interledger.openpayments.MandateId;

import org.immutables.value.Value;

/**
 * Event that is emitted when a mandate is approved by the user.
 */
@Value.Immutable
public interface MandateApprovedEvent {

  static ImmutableMandateApprovedEvent.Builder builder() {
    return ImmutableMandateApprovedEvent.builder();
  }

  AccountId accountId();

  MandateId mandateId();
}
