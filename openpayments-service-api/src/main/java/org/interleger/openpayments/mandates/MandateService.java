package org.interleger.openpayments.mandates;

import org.interledger.openpayments.Charge;
import org.interledger.openpayments.ChargeId;
import org.interledger.openpayments.Mandate;
import org.interledger.openpayments.MandateId;
import org.interledger.openpayments.NewCharge;
import org.interledger.openpayments.NewMandate;
import org.interledger.openpayments.PayIdAccountId;

import java.util.List;
import java.util.Optional;

public interface MandateService {
  Mandate createMandate(PayIdAccountId payIdAccountId, NewMandate newMandate);

  Optional<Mandate> findMandateById(PayIdAccountId payIdAccountId, MandateId mandateId);

  Charge createCharge(PayIdAccountId payIdAccountId, MandateId mandateId, NewCharge newCharge);

  Optional<Charge> findChargeById(PayIdAccountId payIdAccountId, MandateId mandateId, ChargeId chargeId);

  List<Mandate> findMandatesByAccountId(PayIdAccountId payIdAccountId);
}
