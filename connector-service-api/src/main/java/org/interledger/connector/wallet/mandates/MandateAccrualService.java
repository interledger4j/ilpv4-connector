package org.interledger.connector.wallet.mandates;

import org.interledger.connector.opa.model.Mandate;

import com.google.common.primitives.UnsignedLong;

public interface MandateAccrualService {

  UnsignedLong calculateBalance(Mandate mandate);

}
