package org.interledger.connector.wallet.mandates;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.opa.model.Mandate;
import org.interledger.connector.opa.model.MandateId;
import org.interledger.connector.opa.model.NewMandate;

import com.google.common.primitives.UnsignedLong;

import java.util.Optional;

public interface MandateService {
  Mandate createMandate(AccountId accountId, NewMandate newMandate);

  Optional<Mandate> findMandateById(MandateId mandateId);

  boolean chargeMandate(MandateId mandateId, UnsignedLong amount);
}
