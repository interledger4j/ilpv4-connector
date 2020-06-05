package org.interleger.openpayments.mandates;

import org.interledger.openpayments.Mandate;

import com.google.common.primitives.UnsignedLong;

public interface MandateAccrualService {

  UnsignedLong calculateBalance(Mandate mandate);

}
