package org.interledger.connector.wallet.mandates;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.opa.model.Charge;
import org.interledger.connector.opa.model.ChargeId;
import org.interledger.connector.opa.model.Mandate;
import org.interledger.connector.opa.model.MandateId;
import org.interledger.connector.opa.model.NewCharge;
import org.interledger.connector.opa.model.NewMandate;

import java.util.List;
import java.util.Optional;

public interface MandateService {
  Mandate createMandate(AccountId accountId, NewMandate newMandate);

  Optional<Mandate> findMandateById(AccountId accountId, MandateId mandateId);

  Charge createCharge(AccountId accountId, MandateId mandateId, NewCharge newCharge);

  Optional<Charge> findChargeById(AccountId accountId, MandateId mandateId, ChargeId chargeId);

  List<Mandate> findMandatesByAccountId(AccountId accountId);
}
