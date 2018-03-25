package com.sappenin.ilpv4.connector.routing;

import org.interledger.core.InterledgerAddress;
import org.junit.Test;

import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.fail;

/**
 * Unit tests for {@link Route}.
 */
public class RouteTest {

  private static final InterledgerAddress GLOBAL_TARGET_PREFIX = InterledgerAddress.of("g.");
  private static final InterledgerAddress CONNECTOR_ACCOUNT_BOB = InterledgerAddress.of("g.mainhub.bob");
  private static final InterledgerAddress CONNECTOR_ACCOUNT_CONNIE = InterledgerAddress.of("g.mainhub.connie");

  private static final Pattern ACCEPT_ALL_SOURCES_PATTERN = Pattern.compile("(.*?)");
  private static final Pattern ACCEPT_NO_SOURCES_PATTERN = Pattern.compile("(.*)");

  @Test
  public void testDefaultValues() {
    final Route route1 = ImmutableRoute.builder()
      .targetPrefix(GLOBAL_TARGET_PREFIX)
      .nextHopLedgerAccount(CONNECTOR_ACCOUNT_CONNIE)
      .build();

    assertThat(route1.getTargetPrefix(), is(GLOBAL_TARGET_PREFIX));
    assertThat(route1.getNextHopLedgerAccount(), is(CONNECTOR_ACCOUNT_CONNIE));
    assertThat(route1.getExpiresAt().isPresent(), is(false));
    assertThat(route1.getSourcePrefixRestrictionRegex().pattern(), is(ACCEPT_ALL_SOURCES_PATTERN.pattern()));
  }

  @Test
  public void testEqualsHashCode() {
    final Route route1 = ImmutableRoute.builder()
      .targetPrefix(GLOBAL_TARGET_PREFIX)
      .nextHopLedgerAccount(CONNECTOR_ACCOUNT_CONNIE)
      .build();
    final Route route2 = ImmutableRoute.builder()
      .targetPrefix(GLOBAL_TARGET_PREFIX)
      .nextHopLedgerAccount(CONNECTOR_ACCOUNT_CONNIE)
      .build();

    final Route route3 = ImmutableRoute.builder()
      .targetPrefix(GLOBAL_TARGET_PREFIX)
      .nextHopLedgerAccount(CONNECTOR_ACCOUNT_CONNIE)
      .sourcePrefixRestrictionRegex(ACCEPT_NO_SOURCES_PATTERN)
      .build();

    assertThat(route1, is(route2));
    assertThat(route2, is(route1));
    assertThat(route1.hashCode(), is(route2.hashCode()));


    assertThat(route1, is(route2));
    assertThat(route2, is(route1));
    assertThat(route1.hashCode(), is(route2.hashCode()));

    assertThat(route1, is(route3));
    assertThat(route2, is(route3));
    assertThat(route1.hashCode(), is(route3.hashCode()));

    assertThat(route2, is(route3));
    assertThat(route2, is(route3));
    assertThat(route2.hashCode(), is(route3.hashCode()));
  }

  @Test
  public void testNotEqualsHashCode() {
    final Route route1 = ImmutableRoute.builder()
      .targetPrefix(GLOBAL_TARGET_PREFIX)
      .nextHopLedgerAccount(CONNECTOR_ACCOUNT_BOB)
      .build();
    final Route route2 = ImmutableRoute.builder()
      .targetPrefix(GLOBAL_TARGET_PREFIX)
      .nextHopLedgerAccount(CONNECTOR_ACCOUNT_CONNIE)
      .sourcePrefixRestrictionRegex(ACCEPT_NO_SOURCES_PATTERN)
      .build();
    final Route route3 = ImmutableRoute.builder()
      .targetPrefix(GLOBAL_TARGET_PREFIX)
      .nextHopLedgerAccount(CONNECTOR_ACCOUNT_CONNIE)
      .sourcePrefixRestrictionRegex(ACCEPT_NO_SOURCES_PATTERN)
      .build();

    assertThat(route1, is(not(route2)));
    assertThat(route2, is(not(route1)));
    assertThat(route1.hashCode(), is(not(route2.hashCode())));

    assertThat(route1, is(not(route3)));
    assertThat(route1, is(not(route3)));
    assertThat(route1.hashCode(), is(not(route3.hashCode())));

    assertThat(route2, is(route3));
    assertThat(route2, is(route3));
    assertThat(route2.hashCode(), is(route3.hashCode()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testTargetAddressNotPrefix() {
    try {
      ImmutableRoute.builder()
        .targetPrefix(CONNECTOR_ACCOUNT_CONNIE)
        .nextHopLedgerAccount(CONNECTOR_ACCOUNT_CONNIE)
        .build();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(),
        is("InterledgerAddress 'g.mainhub.connie' must be an Address Prefix ending with a dot (.)"));
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNextHopAccountIsPrefix() {
    try {
      ImmutableRoute.builder()
        .targetPrefix(GLOBAL_TARGET_PREFIX)
        .nextHopLedgerAccount(GLOBAL_TARGET_PREFIX)
        .build();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(),
        is("InterledgerAddress 'g.' must NOT be an Address Prefix ending with a dot (.)"));
      throw e;
    }
  }

}