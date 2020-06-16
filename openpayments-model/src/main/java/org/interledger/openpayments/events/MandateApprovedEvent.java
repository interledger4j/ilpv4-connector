package org.interledger.openpayments.events;

import org.interledger.openpayments.MandateId;
import org.interledger.openpayments.PayIdAccountId;

import org.immutables.value.Value;

/**
 * Event that is emitted when a mandate is approved by the user.
 */
@Value.Immutable
public interface MandateApprovedEvent {

  static ImmutableMandateApprovedEvent.Builder builder() {
    return ImmutableMandateApprovedEvent.builder();
  }

  PayIdAccountId accountId();

  MandateId mandateId();
}
