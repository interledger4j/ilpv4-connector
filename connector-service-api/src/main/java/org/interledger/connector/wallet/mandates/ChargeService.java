package org.interledger.connector.wallet.mandates;

import org.interledger.connector.opa.model.Charge;
import org.interledger.connector.opa.model.MandateId;
import org.interledger.connector.opa.model.NewCharge;

public interface ChargeService {

  Charge createCharge(MandateId mandateId, NewCharge newCharge);

}
