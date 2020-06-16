package org.interledger.openpayments.mandates;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.accounts.AccountId;
import org.interledger.openpayments.Charge;
import org.interledger.openpayments.ChargeId;
import org.interledger.openpayments.ChargeStatus;
import org.interledger.openpayments.ImmutableCharge;
import org.interledger.openpayments.ImmutableMandate;
import org.interledger.openpayments.Mandate;
import org.interledger.openpayments.MandateId;
import org.interledger.openpayments.MandateStatus;
import org.interledger.openpayments.MutableClock;
import org.interledger.openpayments.PaymentNetwork;

import com.google.common.primitives.UnsignedLong;
import okhttp3.HttpUrl;
import org.interleger.openpayments.mandates.MandateAccrualService;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class DefaultMandateAccrualServiceTest {

  private static final AccountId ACCOUNT_ID = AccountId.of("alice");
  private Instant startsAt = Instant.now();

  private MutableClock mutableClock;

  private MandateAccrualService service;

  @Before
  public void setUp() {
    mutableClock = new MutableClock();
    service = new DefaultMandateAccrualService(mutableClock);
  }

  @Test
  public void calculateAccruedAmount_NoInterval_NoExpiresAt() {
    Mandate mandate = mandateBuilder().build();

    mutableClock.instant(startsAt.minus(Duration.ofDays(1)));
    // no balance before started
    assertThat(service.calculateBalance(mandate)).isEqualTo(UnsignedLong.ZERO);

    mutableClock.instant(startsAt);
    assertThat(service.calculateBalance(mandate)).isEqualTo(mandate.amount());

    mutableClock.advanceBy(Duration.ofDays(1));
    assertThat(service.calculateBalance(mandate)).isEqualTo(mandate.amount());
  }

  @Test
  public void calculateAccruedAmount_WithInterval_NoExpiresAt() {
    Mandate mandate = mandateBuilder()
      .interval(Duration.ofDays(1))
      .build();

    mutableClock.instant(startsAt.minus(Duration.ofDays(1)));
    // no balance before started
    assertThat(service.calculateBalance(mandate)).isEqualTo(UnsignedLong.ZERO);

    mutableClock.instant(startsAt);
    assertThat(service.calculateBalance(mandate)).isEqualTo(mandate.amount());

    mutableClock.instant(startsAt.plus(Duration.ofHours(23)));
    assertThat(service.calculateBalance(mandate)).isEqualTo(mandate.amount());

    mutableClock.instant(startsAt.plus(Duration.ofDays(1)));
    assertThat(service.calculateBalance(mandate)).isEqualTo(mandate.amount().times(UnsignedLong.valueOf(2)));

    mutableClock.instant(startsAt.plus(Duration.ofDays(10)));
    assertThat(service.calculateBalance(mandate)).isEqualTo(mandate.amount().times(UnsignedLong.valueOf(11)));
  }

  @Test
  public void calculateAccruedAmount_WithInterval_WithExpiresAt() {
    Mandate mandate = mandateBuilder()
      .interval(Duration.ofDays(1))
      .expiresAt(startsAt.plus(Duration.ofDays(10)))
      .build();

    mutableClock.instant(startsAt.minus(Duration.ofDays(1)));
    // no balance before started
    assertThat(service.calculateBalance(mandate)).isEqualTo(UnsignedLong.ZERO);

    mutableClock.instant(startsAt);
    assertThat(service.calculateBalance(mandate)).isEqualTo(mandate.amount());

    mutableClock.instant(startsAt.plus(Duration.ofHours(23)));
    assertThat(service.calculateBalance(mandate)).isEqualTo(mandate.amount());

    mutableClock.instant(startsAt.plus(Duration.ofDays(1)));
    assertThat(service.calculateBalance(mandate)).isEqualTo(mandate.amount().times(UnsignedLong.valueOf(2)));

    mutableClock.instant(startsAt.plus(Duration.ofDays(9).plusMillis(1)));
    assertThat(service.calculateBalance(mandate)).isEqualTo(mandate.amount().times(UnsignedLong.valueOf(10)));

    mutableClock.instant(mandate.expiresAt().get().minusMillis(1));
    assertThat(service.calculateBalance(mandate)).isEqualTo(mandate.amount().times(UnsignedLong.valueOf(10)));

    mutableClock.instant(mandate.expiresAt().get()); // right at expiration
    assertThat(service.calculateBalance(mandate)).isEqualTo(mandate.amount().times(UnsignedLong.ZERO));

    mutableClock.instant(mandate.expiresAt().get().plusMillis(1)); // just after expiration
    assertThat(service.calculateBalance(mandate)).isEqualTo(mandate.amount().times(UnsignedLong.ZERO));
  }

  @Test
  public void calculateAccruedAmountPartiallyCharged() {
    Mandate mandate = mandateBuilder()
      .addCharges(chargeBuilder(UnsignedLong.valueOf(10)).build())
      .build();

    assertThat(service.calculateBalance(mandate)).isEqualTo(UnsignedLong.valueOf(90));
  }

  @Test
  public void calculateAccruedAmountFullyCharged() {
    Mandate mandate = mandateBuilder()
      .addCharges(chargeBuilder(UnsignedLong.valueOf(100)).build())
      .build();

    assertThat(service.calculateBalance(mandate)).isEqualTo(UnsignedLong.ZERO);
  }

  @Test
  public void calculateAccruedAmountOverCharged() {
    Mandate mandate = mandateBuilder()
      .addCharges(chargeBuilder(UnsignedLong.valueOf(101)).build())
      .build();

    assertThat(service.calculateBalance(mandate)).isEqualTo(UnsignedLong.ZERO);
  }

  public ImmutableMandate.Builder mandateBuilder() {
    return Mandate.builder()
      .id(HttpUrl.parse("http://localhost/accounts/1/mandates/1"))
      .account(HttpUrl.parse("http://localhost/accounts/1"))
      .accountId(ACCOUNT_ID.value())
      .mandateId(MandateId.of("1"))
      .amount(UnsignedLong.valueOf(100))
      .assetCode("XRP")
      .assetScale((short) 9)
      .paymentNetwork(PaymentNetwork.XRPL)
      .startAt(startsAt)
      .status(MandateStatus.AWAITING_APPROVAL)
      .balance(UnsignedLong.ZERO);
  }

  public ImmutableCharge.Builder chargeBuilder(UnsignedLong amount) {
    ChargeId chargeId = ChargeId.of(UUID.randomUUID().toString());
    return Charge.builder()
      .id(HttpUrl.parse("http://localhost/accounts/1/mandates/1/" + chargeId.value()))
      .chargeId(chargeId)
      .mandate(HttpUrl.parse("http://localhost/accounts/1/mandates/1"))
      .mandateId(MandateId.of("1"))
      .status(ChargeStatus.CREATED)
      .invoice(HttpUrl.parse("http://localhost/invoices/123"))
      .accountId(ACCOUNT_ID)
      .amount(amount);

  }

}