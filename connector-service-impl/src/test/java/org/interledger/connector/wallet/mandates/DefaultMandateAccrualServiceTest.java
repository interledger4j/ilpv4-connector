package org.interledger.connector.wallet.mandates;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.MutableClock;
import org.interledger.connector.opa.model.ImmutableMandate;
import org.interledger.connector.opa.model.Mandate;
import org.interledger.connector.opa.model.MandateId;

import com.google.common.primitives.UnsignedLong;
import okhttp3.HttpUrl;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;

public class DefaultMandateAccrualServiceTest {

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
    Mandate mandate = mandateBuilder()
      .build();

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
      .totalCharged(UnsignedLong.valueOf(10))
      .build();

    assertThat(service.calculateBalance(mandate)).isEqualTo(UnsignedLong.valueOf(90));
  }

  @Test
  public void calculateAccruedAmountFullyCharged() {
    Mandate mandate = mandateBuilder()
      .totalCharged(UnsignedLong.valueOf(100))
      .build();

    assertThat(service.calculateBalance(mandate)).isEqualTo(UnsignedLong.ZERO);
  }

  @Test
  public void calculateAccruedAmountOverCharged() {
    Mandate mandate = mandateBuilder()
      .totalCharged(UnsignedLong.valueOf(101))
      .build();

    assertThat(service.calculateBalance(mandate)).isEqualTo(UnsignedLong.ZERO);
  }

  public ImmutableMandate.Builder mandateBuilder() {
    return Mandate.builder()
      .id(HttpUrl.parse("http://localhost/accounts/1/mandates/1"))
      .account(HttpUrl.parse("http://localhost/accounts/1"))
      .accountId("1")
      .mandateId(MandateId.of("1"))
      .amount(UnsignedLong.valueOf(100))
      .assetCode("XRP")
      .assetScale((short) 9)
      .startAt(startsAt)
      .balance(UnsignedLong.ZERO);
  }
}