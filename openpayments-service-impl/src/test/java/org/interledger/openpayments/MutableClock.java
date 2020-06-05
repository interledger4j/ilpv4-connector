package org.interledger.openpayments;

import static java.lang.String.format;
import static java.util.Objects.hash;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.TemporalAmount;
import java.util.Objects;

/**
 * Clock where time can be set/mutated for testing purposes.
 * Copied from https://github.com/robfletcher/test-clock
 */
public class MutableClock extends Clock {

  private Instant instant;
  private ZoneId zone;

  public MutableClock() {
    this(Instant.now(), ZoneId.systemDefault());
  }

  public MutableClock(Instant instant) {
    this(instant, ZoneId.systemDefault());
  }

  public MutableClock(ZoneId zone) {
    this(Instant.now(), zone);
  }

  public MutableClock(Instant instant, ZoneId zone) {
    this.instant = instant;
    this.zone = zone;
  }

  @Override public Clock withZone(ZoneId zone) {
    return new MutableClock(instant, zone);
  }

  @Override public ZoneId getZone() {
    return zone;
  }

  @Override public Instant instant() {
    return instant;
  }

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || !(o instanceof Clock)) return false;
    Clock that = (Clock) o;
    return Objects.equals(instant, that.instant()) &&
      Objects.equals(zone, that.getZone());
  }

  @Override public int hashCode() {
    return hash(super.hashCode(), instant, zone);
  }

  @Override
  public String toString() {
    return format("MutableClock[%s,%s]", instant, zone);
  }

  public void advanceBy(TemporalAmount amount) {
    instant = instant.plus(amount);
  }

  public void rewindBy(TemporalAmount amount) {
    instant = instant.minus(amount);
  }

  public void instant(Instant newInstant) {
    instant = newInstant;
  }

  public Clock toFixed() {
    return Clock.fixed(instant, zone);
  }
}