package org.interleger.openpayments.mandates;

import org.interledger.connector.accounts.AccountId;
import org.interledger.openpayments.Charge;
import org.interledger.openpayments.ChargeId;
import org.interledger.openpayments.Mandate;
import org.interledger.openpayments.MandateId;
import org.interledger.openpayments.NewCharge;
import org.interledger.openpayments.NewMandate;

import java.util.List;
import java.util.Optional;

public interface MandateService {
  Mandate createMandate(AccountId accountId, NewMandate newMandate);

  Optional<Mandate> findMandateById(AccountId accountId, MandateId mandateId);

  Charge createCharge(AccountId accountId, MandateId mandateId, NewCharge newCharge);

  Optional<Charge> findChargeById(AccountId accountId, MandateId mandateId, ChargeId chargeId);

  List<Mandate> findMandatesByAccountId(AccountId accountId);
}
