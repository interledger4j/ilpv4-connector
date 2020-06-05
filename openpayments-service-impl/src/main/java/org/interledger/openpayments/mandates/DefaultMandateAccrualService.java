package org.interledger.openpayments.mandates;

import org.interledger.openpayments.Mandate;

import com.google.common.primitives.UnsignedLong;
import org.interleger.openpayments.mandates.MandateAccrualService;

import java.math.BigInteger;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public class DefaultMandateAccrualService implements MandateAccrualService {

  private final Clock clock;

  public DefaultMandateAccrualService(Clock clock) {
    this.clock = clock;
  }

  @Override
  public UnsignedLong calculateBalance(Mandate mandate) {
    UnsignedLong accruedAmount = calculateAccruedAmount(mandate);
    if (isOverCharged(mandate, accruedAmount)) {
      return UnsignedLong.ZERO;
    }
    return accruedAmount.minus(mandate.totalCharged());
  }

  public boolean isOverCharged(Mandate mandate, UnsignedLong accruedAmount) {
    return mandate.totalCharged().compareTo(accruedAmount) > 0;
  }

  private UnsignedLong calculateAccruedAmount(Mandate mandate) {
    Instant now = Instant.now(clock);

    boolean expired = isExpired(mandate, now);
    boolean notStarted = now.isBefore(mandate.startAt());

    if (notStarted || expired) {
      return UnsignedLong.ZERO;
    }

    Duration accrualInterval = Duration.between(mandate.startAt(), now.plusNanos(1));
    long intervalMillis = mandate.interval().map(Duration::toMillis)
      .orElse(Long.MAX_VALUE);


    BigInteger accrualPeriods = BigInteger.valueOf(accrualInterval.toMillis())
      .divide(BigInteger.valueOf(intervalMillis))
      .add(BigInteger.ONE);

    return UnsignedLong.valueOf(accrualPeriods.multiply(mandate.amount().bigIntegerValue()));
  }

  private Boolean isExpired(Mandate mandate, Instant now) {
    return mandate.expiresAt()
      .map(expiresAt -> !now.isBefore(expiresAt))
      .orElse(false);
  }
}
