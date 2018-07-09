package com.sappenin.ilpv4.connector.routing;

import org.interledger.core.InterledgerAddress;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;

/**
 * Unit tests for {@link InterledgerPrefixMap}.
 */
public class InterledgerAddressPrefixMapTest {

  private static final InterledgerAddress GLOBAL_ROUTE = InterledgerAddress.of("g.");
  private static final Pattern ACCEPT_SOME_SOURCES_PATTERN = Pattern.compile("g\\.usd\\.(.*)");
  private static final Pattern ACCEPT_NO_SOURCES_PATTERN = Pattern.compile("(.*)");
  private static final InterledgerAddress DEFAULT_TARGET_ADDRESS_PREFIX = GLOBAL_ROUTE;
  private static final InterledgerAddress DEFAULT_CONNECTOR_ACCOUNT = InterledgerAddress.of("g.mainhub.connie");
  private static final InterledgerAddress ROUTE0_TARGET_PREFIX = GLOBAL_ROUTE;
  private static final InterledgerAddress ROUTE1_TARGET_PREFIX = GLOBAL_ROUTE.with("foo.");
  private static final InterledgerAddress ROUTE2_TARGET_PREFIX = GLOBAL_ROUTE.with("bar.");

  ////////////////////
  // Test GetSize
  ////////////////////
  private static final InterledgerAddress ROUTE3_TARGET_PREFIX = GLOBAL_ROUTE.with("baz.boo.");

  ////////////////////
  // Test AddRoute
  ////////////////////
  private static final InterledgerAddress ROUTE4_TARGET_PREFIX = GLOBAL_ROUTE.with("baz.boo.bar.");
  private static final InterledgerAddress ROUTE5_TARGET_PREFIX = GLOBAL_ROUTE.with("baz.boo.bar.");
  private static final InterledgerAddress ROUTE5_CONNECTOR_ACCOUNT = DEFAULT_CONNECTOR_ACCOUNT.with("somethingelse");

  ////////////////////
  // Test RemoveRoute
  ////////////////////
  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private InterledgerPrefixMap<Route> prefixMap;

  @Before
  public void setup() {
    this.prefixMap = new InterledgerPrefixMap<>();
  }

  @Test
  public void testGetSize() {
    for (int i = 1; i <= 10; i++) {
      final Route route = ImmutableRoute.builder()
        .targetPrefix(InterledgerAddress.of("g." + i + "."))
        .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT)
        .build();
      prefixMap.add(route);
      assertThat(prefixMap.getNumKeys(), is(i));
    }
  }

  @Test(expected = NullPointerException.class)
  public void testAddRouteNull() {
    try {
      prefixMap.add(null);
    } catch (NullPointerException npe) {
      assertThat(npe.getMessage(), is(nullValue()));
      throw npe;
    }
  }

  ////////////////////
  // Test RemoveRoutes
  ////////////////////

  @Test
  public void testAddSameRouteMultipleTimes() {
    final Route globalRoute = ImmutableRoute.builder()
      .targetPrefix(DEFAULT_TARGET_ADDRESS_PREFIX)
      .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT)
      .build();

    for (int i = 0; i < 10; i++) {
      prefixMap.add(globalRoute);
      assertThat("Duplicate Route Keys should not be added more than once!", prefixMap.getNumKeys(), is(1));
      assertThat("Duplicate Routes should not be added more than once!", prefixMap.getRoutes
        (DEFAULT_TARGET_ADDRESS_PREFIX).size(), is(1));
    }
  }

  ////////////////////
  // Test GetRoutes
  ////////////////////

  @Test
  public void testAddFilteredRouteMultipleTimes() {
    final Route globalRoute = ImmutableRoute.builder()
      .targetPrefix(DEFAULT_TARGET_ADDRESS_PREFIX)
      .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT)
      .build();

    final Route filteredRoute = ImmutableRoute.builder()
      .targetPrefix(DEFAULT_TARGET_ADDRESS_PREFIX)
      .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT)
      // Use this for differentiation because it's different from the default pattern.
      .sourcePrefixRestrictionRegex(ACCEPT_NO_SOURCES_PATTERN)
      .build();

    for (int i = 0; i < 10; i++) {
      prefixMap.add(globalRoute);
      prefixMap.add(filteredRoute);
      assertThat("Duplicate Route Keys should not be added more than once!", prefixMap.getNumKeys(), is(1));
      assertThat("Duplicate Routes should not be added more than once!", prefixMap.getRoutes
        (DEFAULT_TARGET_ADDRESS_PREFIX).size(), is(2));
    }
  }

  @Test(expected = NullPointerException.class)
  public void testRemoveRouteNull() {
    try {
      prefixMap.removeRoute(null);
    } catch (NullPointerException npe) {
      assertThat(npe.getMessage(), is(nullValue()));
      throw npe;
    }
  }

  /**
   * Helper method to get a matching route from the prefix map, which might hold multiple routes for a given
   * targetPrefix.
   */
  private Route getMatchingRoute(InterledgerAddress targetPrefix, Optional<InterledgerAddress> nextHopAccount) {
    // This logic assumes that if nextHopAccount is unspecified, then the next-hop connector account is
    // DEFAULT_CONNECTOR_ACCOUNT
    final InterledgerAddress nextHopEqualityCheck = nextHopAccount.orElse(DEFAULT_CONNECTOR_ACCOUNT);
    return this.prefixMap.getRoutes(targetPrefix).stream()
      .filter(r -> r.getNextHopAccount().equals(nextHopEqualityCheck))
      .findFirst().get();
  }

  ////////////////////
  // Test ForEach
  ////////////////////

  /**
   * Helper method to get a matching route from the prefix map, which might hold multiple routes for a given
   * targetPrefix.
   */
  private Route getMatchingRoute(InterledgerAddress targetPrefix, InterledgerAddress nextHopAccount) {
    return getMatchingRoute(targetPrefix, Optional.of(nextHopAccount));
  }

  ////////////////////
  // TEST getPrefixMapKeys
  ////////////////////

  /**
   * Helper method to get a matching route from the prefix map, which might hold multiple routes for a given
   * targetPrefix.
   */
  private Route getMatchingRoute(InterledgerAddress targetPrefix) {
    return getMatchingRoute(targetPrefix, Optional.empty());
  }

  ////////////////////
  // Test GetRoutes
  ////////////////////

  @Test
  public void testRemoveRoute() {
    this.prefixMap = constructPopulatedPrefixMap();
    assertThat(prefixMap.getNumKeys(), is(5));

    {
      final Route route0 = this.getMatchingRoute(ROUTE0_TARGET_PREFIX);

      final boolean actual = prefixMap.removeRoute(route0);

      assertThat(actual, is(true));
      assertThat(prefixMap.getRoutes(route0.getTargetPrefix()).isEmpty(), is(true));
      assertThat(prefixMap.getNumKeys(), is(4));
    }
    {
      final Route route1 = this.getMatchingRoute(ROUTE1_TARGET_PREFIX);

      final boolean actual = prefixMap.removeRoute(route1);

      assertThat(actual, is(true));
      assertThat(prefixMap.getRoutes(route1.getTargetPrefix()).isEmpty(), is(true));
      assertThat(prefixMap.getNumKeys(), is(3));
    }
    {
      final Route route2 = this.getMatchingRoute(ROUTE2_TARGET_PREFIX);
      final boolean actual = prefixMap.removeRoute(route2);

      assertThat(actual, is(true));
      assertThat(prefixMap.getRoutes(route2.getTargetPrefix()).isEmpty(), is(true));
      assertThat(prefixMap.getNumKeys(), is(2));
    }
    {
      final Route route3 = this.getMatchingRoute(ROUTE3_TARGET_PREFIX);

      final boolean actual = prefixMap.removeRoute(route3);

      assertThat(actual, is(true));
      assertThat(prefixMap.getRoutes(route3.getTargetPrefix()).isEmpty(), is(true));
      assertThat(prefixMap.getNumKeys(), is(1));
    }
    {
      final Route route4 = this.getMatchingRoute(ROUTE4_TARGET_PREFIX);
      this.prefixMap.forEach((key, value) -> logger.info("K: {}, V: {}", key, value));
      final boolean actual = prefixMap.removeRoute(route4);

      assertThat(actual, is(true));
      assertThat(prefixMap.getRoutes(route4.getTargetPrefix()).size(), is(1));
      assertThat(prefixMap.getNumKeys(), is(1));
      this.prefixMap.forEach((key, value) -> logger.info("K: {}, V: {}", key, value));
    }
    {
      final Route route5 = this.getMatchingRoute(ROUTE5_TARGET_PREFIX, ROUTE5_CONNECTOR_ACCOUNT);

      final boolean actual = prefixMap.removeRoute(route5);

      assertThat(actual, is(true));
      assertThat(prefixMap.getRoutes(route5.getTargetPrefix()).isEmpty(), is(true));
      assertThat(prefixMap.getNumKeys(), is(0));
    }
  }

  @Test
  public void testRemoveRoutes() {
    this.prefixMap = new InterledgerPrefixMap();

    // Multiple identicial routes with different next-hops...
    {
      final Route route0a = ImmutableRoute.builder()
        .targetPrefix(GLOBAL_ROUTE)
        .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT)
        .build();
      this.prefixMap.add(route0a);

      final Route route0b = ImmutableRoute.builder()
        .targetPrefix(GLOBAL_ROUTE)
        .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT.with("ha1"))
        .build();
      this.prefixMap.add(route0b);

      final Route route0c = ImmutableRoute.builder()
        .targetPrefix(GLOBAL_ROUTE)
        .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT.with("ha2"))
        .build();
      this.prefixMap.add(route0c);

      assertThat(prefixMap.getNumKeys(), is(1));

      final Collection<Route> prior = prefixMap.removeAllRoutes(GLOBAL_ROUTE);
      assertThat(prior.size(), is(3));
      assertThat(prior.contains(route0a), is(true));
      assertThat(prior.contains(route0b), is(true));
      assertThat(prior.contains(route0c), is(true));
      assertThat(prefixMap.getRoutes(GLOBAL_ROUTE).isEmpty(), is(true));
      assertThat(prefixMap.getNumKeys(), is(0));
    }

    // Multiple identical routes with different filters...
    {
      final InterledgerAddress targetPrefix = GLOBAL_ROUTE.with("2.");
      final Route route1a = ImmutableRoute.builder()
        .targetPrefix(targetPrefix)
        .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT)
        .build();
      this.prefixMap.add(route1a);

      final Route route1b = ImmutableRoute.builder()
        .targetPrefix(targetPrefix)
        .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT)
        .sourcePrefixRestrictionRegex(ACCEPT_SOME_SOURCES_PATTERN)
        .build();
      this.prefixMap.add(route1b);

      final Route route1c = ImmutableRoute.builder()
        .targetPrefix(targetPrefix)
        .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT)
        .sourcePrefixRestrictionRegex(ACCEPT_NO_SOURCES_PATTERN)
        .build();
      this.prefixMap.add(route1c);

      assertThat(prefixMap.getNumKeys(), is(1));

      final Collection<Route> prior = prefixMap.removeAllRoutes(targetPrefix);

      assertThat(prior.size(), is(3));
      assertThat(prior.contains(route1a), is(true));
      assertThat(prior.contains(route1b), is(true));
      assertThat(prior.contains(route1c), is(true));
      assertThat(prefixMap.getRoutes(targetPrefix).isEmpty(), is(true));
      assertThat(prefixMap.getNumKeys(), is(0));
    }
  }

  @Test(expected = NullPointerException.class)
  public void testGetRoutesNull() {
    try {
      prefixMap.getRoutes(null);
    } catch (NullPointerException npe) {
      assertThat(npe.getMessage(), is("address must not be null!"));
      throw npe;
    }
  }

  @Test
  public void testGetRoutesMultipleTimes() {
    final Route globalRoute = ImmutableRoute.builder()
      .targetPrefix(DEFAULT_TARGET_ADDRESS_PREFIX)
      .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT)
      .build();

    for (int i = 0; i < 10; i++) {
      prefixMap.add(globalRoute);

      final Collection<Route> routes = prefixMap.getRoutes(DEFAULT_TARGET_ADDRESS_PREFIX);

      assertThat("Duplicate Routes should not be added more than once!", routes.size(), is(1));
    }
  }

  @Test
  public void testGetFilteredRouteMultipleTimes() {
    final Route globalRoute = ImmutableRoute.builder()
      .targetPrefix(DEFAULT_TARGET_ADDRESS_PREFIX)
      .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT)
      .build();

    final Route filteredRoute = ImmutableRoute.builder()
      .targetPrefix(DEFAULT_TARGET_ADDRESS_PREFIX)
      .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT)
      .sourcePrefixRestrictionRegex(ACCEPT_NO_SOURCES_PATTERN)
      .build();

    for (int i = 0; i < 10; i++) {
      prefixMap.add(globalRoute);
      prefixMap.add(filteredRoute);

      final Collection<Route> routes = prefixMap.getRoutes(DEFAULT_TARGET_ADDRESS_PREFIX);

      assertThat("Duplicate Routes should not be added more than once!", routes.size(), is(2));
    }
  }

  @Test
  public void testForEach() {
    final Route globalRoute = ImmutableRoute.builder()
      .targetPrefix(DEFAULT_TARGET_ADDRESS_PREFIX)
      .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT)
      .build();
    prefixMap.add(globalRoute);

    final Route globalRoute2 = ImmutableRoute.builder()
      .targetPrefix(DEFAULT_TARGET_ADDRESS_PREFIX.with("foo."))
      .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT)
      .build();
    prefixMap.add(globalRoute2);

    final Route filteredRoute1 = ImmutableRoute.builder()
      .targetPrefix(DEFAULT_TARGET_ADDRESS_PREFIX)
      .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT)
      .sourcePrefixRestrictionRegex(ACCEPT_NO_SOURCES_PATTERN)
      .build();
    prefixMap.add(filteredRoute1);

    final AtomicInteger atomicInteger = new AtomicInteger();
    prefixMap.forEach((targetAddress, route) -> atomicInteger.getAndIncrement());

    assertThat(atomicInteger.get(), is(2));
  }

  @Test
  public void testGetPrefixMapKeys() {
    this.prefixMap = this.constructPopulatedPrefixMap();
    assertThat(this.prefixMap.getPrefixMapKeys().size(), is(5));
  }

  ////////////////////
  // Test findLongestPrefix
  ////////////////////

  @Test
  public void testGetNextHopRouteForEachPrefix() {
    for (int i = 1; i <= 10; i++) {
      final Route route = ImmutableRoute.builder()
        .targetPrefix(InterledgerAddress.of("g." + i + "."))
        .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT)
        .build();
      prefixMap.add(route);

      assertThat(prefixMap.getNumKeys(), is(i));
      final InterledgerAddress destinationAddress = InterledgerAddress.of("g." + i + ".bob");
      assertThat("Each route should be retrieved with only a single value!",
        prefixMap.findNextHopRoutes(destinationAddress).size(), is(1)
      );
    }
  }

  @Test
  public void testGetNextHopRouteWithDuplicateDestinations() {
    for (int i = 1; i <= 10; i++) {
      final Route route = ImmutableRoute.builder()
        .targetPrefix(GLOBAL_ROUTE)
        .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT.with("" + i))
        .build();
      prefixMap.add(route);

      final InterledgerAddress destinationAddress = DEFAULT_TARGET_ADDRESS_PREFIX.with("bob");
      assertThat("Each destination address should map to N number of Routes!",
        prefixMap.findNextHopRoutes(destinationAddress).size() > 0, is(true)
      );

      assertThat("The destination address should return the added Route from the Map!",
        prefixMap.findNextHopRoutes(destinationAddress).stream().filter(r -> r.equals(route))
          .collect(Collectors.toList()).size() > 0, is(true)
      );
    }
  }

  @Test
  public void testGetNextHopRouteWithNoRoutesInMap() {
    assertThat(
      prefixMap.findNextHopRoutes(DEFAULT_TARGET_ADDRESS_PREFIX.with("bob")).size(), is(0)
    );
  }

  ////////////////////
  // test getRootPrefix
  ////////////////////

  @Test
  public void testGetNextHopRouteWithNonMatchingDestination1() {
    final Route route = ImmutableRoute.builder()
      .targetPrefix(GLOBAL_ROUTE)
      .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT)
      .build();

    prefixMap.add(route);

    assertThat(prefixMap.findNextHopRoutes(InterledgerAddress.of("self.me")).size(), is(0));
    assertThat(prefixMap.findNextHopRoutes(InterledgerAddress.of("g.1.me")).size(), is(1));
    assertThat(prefixMap.findNextHopRoutes(InterledgerAddress.of("g.1.m")).size(), is(1));
    assertThat(prefixMap.findNextHopRoutes(InterledgerAddress.of("g.1")).size(), is(1));
    assertThat(prefixMap.findNextHopRoutes(InterledgerAddress.of("g.2")).size(), is(1));
  }

  ////////////////////
  // Private Helpers
  ////////////////////

  @Test
  public void testGetNextHopRouteWithNonMatchingDestination2() {
    final Route route = ImmutableRoute.builder()
      .targetPrefix(GLOBAL_ROUTE.with("foo."))
      .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT)
      .build();

    prefixMap.add(route);

    assertThat(prefixMap.findNextHopRoutes(InterledgerAddress.of("self.me")).size(), is(0));
    assertThat(prefixMap.findNextHopRoutes(InterledgerAddress.of("g.1.me")).size(), is(0));
    assertThat(prefixMap.findNextHopRoutes(InterledgerAddress.of("g.1.m")).size(), is(0));
    assertThat(prefixMap.findNextHopRoutes(InterledgerAddress.of("g.1")).size(), is(0));
    assertThat(prefixMap.findNextHopRoutes(InterledgerAddress.of("g.2")).size(), is(0));
  }

  @Test
  public void testGetNextHopRouteWithDifferringLengthsInTable() {
    this.prefixMap = this.constructPopulatedPrefixMap();

    assertThat(prefixMap.findNextHopRoutes(InterledgerAddress.of("self.me")).size(), is(0));
    assertThat(prefixMap.findNextHopRoutes(InterledgerAddress.of("g.1.me")).size(), is(1));
    assertThat(prefixMap.findNextHopRoutes(InterledgerAddress.of("g.1.m")).size(), is(1));
    assertThat(prefixMap.findNextHopRoutes(InterledgerAddress.of("g.1")).size(), is(1));
    assertThat(prefixMap.findNextHopRoutes(InterledgerAddress.of("g.2")).size(), is(1));
    assertThat(prefixMap.findNextHopRoutes(InterledgerAddress.of("g.foo.bob")).size(), is(1));
    assertThat(prefixMap.findNextHopRoutes(InterledgerAddress.of("g.bar.bob")).size(), is(1));
    assertThat(prefixMap.findNextHopRoutes(InterledgerAddress.of("g.baz.boo.alice")).size(), is(1));
    assertThat(prefixMap.findNextHopRoutes(InterledgerAddress.of("g.baz.boo.bob")).size(), is(1));
    assertThat(prefixMap.findNextHopRoutes(InterledgerAddress.of("g.baz.boo.bar.alice")).size(), is(2));
    assertThat(prefixMap.findNextHopRoutes(InterledgerAddress.of("g.baz.boo.bar.bob")).size(), is(2));
  }

  @Test
  public void testGetNextHopRoutesValidateReturnedRoutes() {
    this.prefixMap = this.constructPopulatedPrefixMap();

    final Route newRoute1 = ImmutableRoute.builder()
      .targetPrefix(GLOBAL_ROUTE.with("unittest."))
      .nextHopAccount(InterledgerAddress.of("g.this.account1"))
      .build();
    prefixMap.add(newRoute1);
    Collection<Route> routes = prefixMap.findNextHopRoutes(InterledgerAddress.of("g.unittest.receiver"));
    assertThat(routes.contains(newRoute1), is(true));

    final Route newRoute2 = ImmutableRoute.builder()
      .targetPrefix(GLOBAL_ROUTE.with("unittest."))
      .nextHopAccount(InterledgerAddress.of("g.this.account2"))
      .build();
    prefixMap.add(newRoute2);

    routes = prefixMap.findNextHopRoutes(InterledgerAddress.of("g.unittest.receiver"));
    assertThat(routes.contains(newRoute1), is(true));
    assertThat(routes.contains(newRoute2), is(true));
  }

  @Test(expected = NullPointerException.class)
  public void testFindLongestPrefixWithNull() {
    this.prefixMap = constructPopulatedPrefixMap();
    try {
      prefixMap.findLongestPrefix(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("addressPrefix must not be null!"));
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFindLongestPrefixWithNonPrefix() {
    this.prefixMap = constructPopulatedPrefixMap();
    try {
      prefixMap.findLongestPrefix(InterledgerAddress.of("g.foo"));
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(),
        is("InterledgerAddress 'g.foo' must be an Address Prefix ending with a dot (.)"));
      throw e;
    }
  }

  @Test
  public void testFindLongestPrefix() {
    this.prefixMap = constructPopulatedPrefixMap();

    // g.
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.b.")).get(), is("g."));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.bo.")).get(), is("g."));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.bob.")).get(), is("g."));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.b.")).get(), is("g."));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.f.")).get(), is("g."));
    // contains g.foo, but then some...
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.fool.")).get(), is("g."));

    // g.foo.
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.foo.a.")).get(), is("g.foo."));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.foo.b.")).get(), is("g.foo."));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.foo.bo.")).get(), is("g.foo."));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.foo.alice.")).get(), is("g.foo."));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.foo.bob.")).get(), is("g.foo."));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.foo.foo.")).get(), is("g.foo."));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.foo.bar.")).get(), is("g.foo."));
    // contains g.foo, but then some...
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.foo.fool.")).get(), is("g.foo."));

    // g.bar.
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.bar.a.")).get(), is("g.bar."));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.bar.b.")).get(), is("g.bar."));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.bar.bo.")).get(), is("g.bar."));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.bar.alice.")).get(), is("g.bar."));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.bar.bob.")).get(), is("g.bar."));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.bar.foo.")).get(), is("g.bar."));
    // contains g.bar, but then some...
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.bart.")).get(), is("g."));

    // g.baz.boo.
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.baz.boo.a.")).get(), is("g.baz.boo."));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.baz.boo.b.")).get(), is("g.baz.boo."));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.baz.boo.bo.")).get(), is("g.baz.boo."));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.baz.boo.alice.")).get(), is("g.baz.boo."));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.baz.boo.bob.")).get(), is("g.baz.boo."));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.baz.boo.foo.")).get(), is("g.baz.boo."));
    // contains g.baz.boo., but then some...
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.baz.bool.")).get(), is("g."));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.bazl.")).get(), is("g."));

    // g.baz.boo.bar.
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.baz.boo.bar.a.")).get(), is("g.baz.boo.bar."));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.baz.boo.bar.b.")).get(), is("g.baz.boo.bar."));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.baz.boo.bar.bo.")).get(), is("g.baz.boo.bar"
      + "."));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.baz.boo.bar.alice.")).get(),
      is("g.baz.boo.bar."));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.baz.boo.bar.bob.")).get(), is("g.baz.boo.bar"
      + "."));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.baz.boo.bar.foo.")).get(), is("g.baz.boo.bar"
      + "."));
    // contains g.baz.boo.bar., but then some...
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.baz.bool.bart.")).get(), is("g."));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.bazl.boo.bar.")).get(), is("g."));

    // g.notfound --> Absent
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.notfound.a.")).get(), is("g."));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.notfound.b.")).get(), is("g."));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.notfound.bo.")).get(), is("g."));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.notfound.alice.")).get(), is("g."));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.notfound.bob.")).get(), is("g."));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.notfound.foo.")).get(), is("g."));

    // g.1. --> g.
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.1.b.")).get(), is("g."));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.1.bo.")).get(), is("g."));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.1.bob.")).get(), is("g."));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.1.b.")).get(), is("g."));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.1.f.")).get(), is("g."));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.1.foo.")).get(), is("g."));
    // contains g.baz.boo.bar., but then some...
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.11.")).get(), is("g."));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddress.of("g.22.")).get(), is("g."));
  }

  @Test
  public void getRootPrefix() {
    this.prefixMap = constructPopulatedPrefixMap();

    assertThat(prefixMap.getRootPrefix(InterledgerAddress.of("g.baz.")).getValue(), is("g."));
    assertThat(prefixMap.getRootPrefix(InterledgerAddress.of("g.baz.bool.")).getValue(), is("g."));
    assertThat(prefixMap.getRootPrefix(InterledgerAddress.of("g.")).getValue(), is("g."));
  }

  private InterledgerPrefixMap constructPopulatedPrefixMap() {
    final Route route0 = ImmutableRoute.builder()
      .targetPrefix(ROUTE0_TARGET_PREFIX)
      .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT)
      .build();

    final Route route1 = ImmutableRoute.builder()
      .targetPrefix(ROUTE1_TARGET_PREFIX)
      .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT)
      .build();

    final Route route2 = ImmutableRoute.builder()
      .targetPrefix(ROUTE2_TARGET_PREFIX)
      .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT)
      .build();

    final Route route3 = ImmutableRoute.builder()
      .targetPrefix(ROUTE3_TARGET_PREFIX)
      .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT)
      .build();

    final Route route4 = ImmutableRoute.builder()
      .targetPrefix(ROUTE4_TARGET_PREFIX)
      .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT)
      .build();

    final Route route5 = ImmutableRoute.builder()
      .targetPrefix(ROUTE5_TARGET_PREFIX)
      .nextHopAccount(ROUTE5_CONNECTOR_ACCOUNT)
      .build();

    return constructTestPrefixMapWithRoutes(route5, route4, route3, route2, route1, route0);
  }

  private InterledgerPrefixMap constructTestPrefixMapWithRoutes(final Route... route) {
    final InterledgerPrefixMap testMap = new InterledgerPrefixMap();

    for (int i = 0; i < route.length; i++) {
      testMap.add(route[i]);
    }

    return testMap;
  }

}