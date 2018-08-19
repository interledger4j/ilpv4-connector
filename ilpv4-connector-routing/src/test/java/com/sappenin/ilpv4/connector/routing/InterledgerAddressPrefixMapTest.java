package com.sappenin.ilpv4.connector.routing;

import com.sappenin.ilpv4.InterledgerAddressPrefix;
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
 * Unit tests for {@link InterledgerAddressPrefixMap}.
 */
public class InterledgerAddressPrefixMapTest {

  private static final InterledgerAddressPrefix GLOBAL_ROUTING_TABLE_ENTRY = InterledgerAddressPrefix.of("g");
  private static final Pattern ACCEPT_SOME_SOURCES_PATTERN = Pattern.compile("g\\.usd\\.(.*)");
  private static final Pattern ACCEPT_NO_SOURCES_PATTERN = Pattern.compile("(.*)");
  private static final InterledgerAddressPrefix DEFAULT_TARGET_ADDRESS_PREFIX = GLOBAL_ROUTING_TABLE_ENTRY;
  private static final InterledgerAddress DEFAULT_CONNECTOR_ACCOUNT = InterledgerAddress.of("g.mainhub.connie");
  private static final InterledgerAddressPrefix ROUTING_TABLE_ENTRY0_TARGET_PREFIX = GLOBAL_ROUTING_TABLE_ENTRY;
  private static final InterledgerAddressPrefix ROUTING_TABLE_ENTRY1_TARGET_PREFIX =
    GLOBAL_ROUTING_TABLE_ENTRY.with("foo");
  private static final InterledgerAddressPrefix ROUTING_TABLE_ENTRY2_TARGET_PREFIX =
    GLOBAL_ROUTING_TABLE_ENTRY.with("bar");

  ////////////////////
  // Test GetSize
  ////////////////////
  private static final InterledgerAddressPrefix ROUTING_TABLE_ENTRY3_TARGET_PREFIX =
    GLOBAL_ROUTING_TABLE_ENTRY.with("baz.boo");

  ////////////////////
  // Test AddRoutingTableEntry
  ////////////////////
  private static final InterledgerAddressPrefix ROUTING_TABLE_ENTRY4_TARGET_PREFIX =
    GLOBAL_ROUTING_TABLE_ENTRY.with("baz.boo.bar");
  private static final InterledgerAddressPrefix ROUTING_TABLE_ENTRY5_TARGET_PREFIX =
    GLOBAL_ROUTING_TABLE_ENTRY.with("baz.boo.bar");
  private static final InterledgerAddress ROUTING_TABLE_ENTRY5_CONNECTOR_ACCOUNT =
    DEFAULT_CONNECTOR_ACCOUNT.with("somethingelse");

  ////////////////////
  // Test RemoveRoutingTableEntry
  ////////////////////
  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private InterledgerAddressPrefixMap<RoutingTableEntry> prefixMap;

  @Before
  public void setup() {
    this.prefixMap = new InterledgerAddressPrefixMap<>();
  }

  @Test
  public void testGetSize() {
    for (int i = 1; i <= 10; i++) {
      final RoutingTableEntry routingTableEntry = ImmutableRoutingTableEntry.builder()
        .targetPrefix(InterledgerAddressPrefix.of("g." + i))
        .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT)
        .build();
      prefixMap.add(routingTableEntry);
      assertThat(prefixMap.getNumKeys(), is(i));
    }
  }

  @Test(expected = NullPointerException.class)
  public void testAddRoutingTableEntryNull() {
    try {
      prefixMap.add(null);
    } catch (NullPointerException npe) {
      assertThat(npe.getMessage(), is(nullValue()));
      throw npe;
    }
  }

  ////////////////////
  // Test RemoveRoutingTableEntrys
  ////////////////////

  @Test
  public void testAddSameRoutingTableEntryMultipleTimes() {
    final RoutingTableEntry globalRoutingTableEntry = ImmutableRoutingTableEntry.builder()
      .targetPrefix(DEFAULT_TARGET_ADDRESS_PREFIX)
      .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT)
      .build();

    for (int i = 0; i < 10; i++) {
      prefixMap.add(globalRoutingTableEntry);
      assertThat("Duplicate RoutingTableEntry Keys should not be added more than once!", prefixMap.getNumKeys(), is(1));
      assertThat("Duplicate RoutingTableEntrys should not be added more than once!", prefixMap.getEntries
        (DEFAULT_TARGET_ADDRESS_PREFIX).size(), is(1));
    }
  }

  ////////////////////
  // Test GetRoutingTableEntrys
  ////////////////////

  @Test
  public void testAddFilteredRoutingTableEntryMultipleTimes() {
    final RoutingTableEntry globalRoutingTableEntry = ImmutableRoutingTableEntry.builder()
      .targetPrefix(DEFAULT_TARGET_ADDRESS_PREFIX)
      .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT)
      .build();

    final RoutingTableEntry filteredRoutingTableEntry =
      ImmutableRoutingTableEntry.builder()
        .from(globalRoutingTableEntry)
        // Use this for differentiation because it's different from the default pattern.
        .sourcePrefixRestrictionRegex(ACCEPT_NO_SOURCES_PATTERN)
        .build();

    for (int i = 0; i < 10; i++) {
      prefixMap.add(globalRoutingTableEntry);
      prefixMap.add(filteredRoutingTableEntry);
      assertThat("Duplicate RoutingTableEntry Keys should not be added more than once!", prefixMap.getNumKeys(), is(1));
      assertThat("Duplicate RoutingTableEntry should not be added more than once!",
        prefixMap.getEntries(DEFAULT_TARGET_ADDRESS_PREFIX).size(), is(2));
    }
  }

  @Test(expected = NullPointerException.class)
  public void testRemoveRoutingTableEntryNull() {
    try {
      prefixMap.remove(null);
    } catch (NullPointerException npe) {
      assertThat(npe.getMessage(), is(nullValue()));
      throw npe;
    }
  }

  /**
   * Helper method to get a matching routingTableEntry from the prefix map, which might hold multiple routingTableEntrys
   * for a given targetPrefix.
   */
  private RoutingTableEntry getMatchingRoutingTableEntry(
    InterledgerAddressPrefix targetPrefix, Optional<InterledgerAddress> nextHopAccount
  ) {
    // This logic assumes that if nextHopAccount is unspecified, then the next-hop connector account is
    // DEFAULT_CONNECTOR_ACCOUNT
    final InterledgerAddress nextHopEqualityCheck = nextHopAccount.orElse(DEFAULT_CONNECTOR_ACCOUNT);
    return this.prefixMap.getEntries(targetPrefix).stream()
      .filter(r -> r.getNextHopAccount().equals(nextHopEqualityCheck))
      .findFirst().get();
  }

  ////////////////////
  // Test ForEach
  ////////////////////

  /**
   * Helper method to get a matching routingTableEntry from the prefix map, which might hold multiple routingTableEntrys
   * for a given targetPrefix.
   */
  private RoutingTableEntry getMatchingRoutingTableEntry(
    InterledgerAddressPrefix targetPrefix, InterledgerAddress nextHopAccount
  ) {
    return getMatchingRoutingTableEntry(targetPrefix, Optional.of(nextHopAccount));
  }

  ////////////////////
  // TEST getPrefixMapKeys
  ////////////////////

  /**
   * Helper method to get a matching routingTableEntry from the prefix map, which might hold multiple routingTableEntrys
   * for a given targetPrefix.
   */
  private RoutingTableEntry getMatchingRoutingTableEntry(InterledgerAddressPrefix targetPrefix) {
    return getMatchingRoutingTableEntry(targetPrefix, Optional.empty());
  }

  ////////////////////
  // Test GetRoutingTableEntrys
  ////////////////////

  @Test
  public void testRemoveRoutingTableEntry() {
    this.prefixMap = constructPopulatedPrefixMap();
    assertThat(prefixMap.getNumKeys(), is(5));

    {
      final RoutingTableEntry routingTableEntry0 =
        this.getMatchingRoutingTableEntry(ROUTING_TABLE_ENTRY0_TARGET_PREFIX);

      final boolean actual = prefixMap.remove(routingTableEntry0);

      assertThat(actual, is(true));
      assertThat(prefixMap.getEntries(routingTableEntry0.getTargetPrefix()).isEmpty(), is(true));
      assertThat(prefixMap.getNumKeys(), is(4));
    }
    {
      final RoutingTableEntry routingTableEntry1 =
        this.getMatchingRoutingTableEntry(ROUTING_TABLE_ENTRY1_TARGET_PREFIX);

      final boolean actual = prefixMap.remove(routingTableEntry1);

      assertThat(actual, is(true));
      assertThat(prefixMap.getEntries(routingTableEntry1.getTargetPrefix()).isEmpty(), is(true));
      assertThat(prefixMap.getNumKeys(), is(3));
    }
    {
      final RoutingTableEntry routingTableEntry2 =
        this.getMatchingRoutingTableEntry(ROUTING_TABLE_ENTRY2_TARGET_PREFIX);
      final boolean actual = prefixMap.remove(routingTableEntry2);

      assertThat(actual, is(true));
      assertThat(prefixMap.getEntries(routingTableEntry2.getTargetPrefix()).isEmpty(), is(true));
      assertThat(prefixMap.getNumKeys(), is(2));
    }
    {
      final RoutingTableEntry routingTableEntry3 =
        this.getMatchingRoutingTableEntry(ROUTING_TABLE_ENTRY3_TARGET_PREFIX);

      final boolean actual = prefixMap.remove(routingTableEntry3);

      assertThat(actual, is(true));
      assertThat(prefixMap.getEntries(routingTableEntry3.getTargetPrefix()).isEmpty(), is(true));
      assertThat(prefixMap.getNumKeys(), is(1));
    }
    {
      final RoutingTableEntry routingTableEntry4 =
        this.getMatchingRoutingTableEntry(ROUTING_TABLE_ENTRY4_TARGET_PREFIX);
      this.prefixMap.forEach((key, value) -> logger.info("K: {}, V: {}", key, value));
      final boolean actual = prefixMap.remove(routingTableEntry4);

      assertThat(actual, is(true));
      assertThat(prefixMap.getEntries(routingTableEntry4.getTargetPrefix()).size(), is(1));
      assertThat(prefixMap.getNumKeys(), is(1));
      this.prefixMap.forEach((key, value) -> logger.info("K: {}, V: {}", key, value));
    }
    {
      final RoutingTableEntry routingTableEntry5 =
        this.getMatchingRoutingTableEntry(ROUTING_TABLE_ENTRY5_TARGET_PREFIX, ROUTING_TABLE_ENTRY5_CONNECTOR_ACCOUNT);

      final boolean actual = prefixMap.remove(routingTableEntry5);

      assertThat(actual, is(true));
      assertThat(prefixMap.getEntries(routingTableEntry5.getTargetPrefix()).isEmpty(), is(true));
      assertThat(prefixMap.getNumKeys(), is(0));
    }
  }

  @Test
  public void testRemoveRoutingTableEntrys() {
    this.prefixMap = new InterledgerAddressPrefixMap();

    // Multiple identicial routingTableEntrys with different next-hops...
    {
      final RoutingTableEntry routingTableEntry0a = ImmutableRoutingTableEntry.builder()
        .targetPrefix(GLOBAL_ROUTING_TABLE_ENTRY)
        .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT)
        .build();
      this.prefixMap.add(routingTableEntry0a);

      final RoutingTableEntry routingTableEntry0b = ImmutableRoutingTableEntry.builder()
        .targetPrefix(GLOBAL_ROUTING_TABLE_ENTRY)
        .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT.with("ha1"))
        .build();
      this.prefixMap.add(routingTableEntry0b);

      final RoutingTableEntry routingTableEntry0c = ImmutableRoutingTableEntry.builder()
        .targetPrefix(GLOBAL_ROUTING_TABLE_ENTRY)
        .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT.with("ha2"))
        .build();
      this.prefixMap.add(routingTableEntry0c);

      assertThat(prefixMap.getNumKeys(), is(1));

      final Collection<RoutingTableEntry> prior = prefixMap.removeAll(GLOBAL_ROUTING_TABLE_ENTRY);
      assertThat(prior.size(), is(3));
      assertThat(prior.contains(routingTableEntry0a), is(true));
      assertThat(prior.contains(routingTableEntry0b), is(true));
      assertThat(prior.contains(routingTableEntry0c), is(true));
      assertThat(prefixMap.getEntries(GLOBAL_ROUTING_TABLE_ENTRY).isEmpty(), is(true));
      assertThat(prefixMap.getNumKeys(), is(0));
    }

    // Multiple identical routingTableEntrys with different filters...
    {
      final InterledgerAddressPrefix targetPrefix = GLOBAL_ROUTING_TABLE_ENTRY.with("2");
      final RoutingTableEntry routingTableEntry1a = ImmutableRoutingTableEntry.builder()
        .targetPrefix(targetPrefix)
        .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT)
        .build();
      this.prefixMap.add(routingTableEntry1a);

      final RoutingTableEntry routingTableEntry1b = ImmutableRoutingTableEntry.builder()
        .targetPrefix(targetPrefix)
        .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT)
        .sourcePrefixRestrictionRegex(ACCEPT_SOME_SOURCES_PATTERN)
        .build();
      this.prefixMap.add(routingTableEntry1b);

      final RoutingTableEntry routingTableEntry1c = ImmutableRoutingTableEntry.builder()
        .targetPrefix(targetPrefix)
        .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT)
        .sourcePrefixRestrictionRegex(ACCEPT_NO_SOURCES_PATTERN)
        .build();
      this.prefixMap.add(routingTableEntry1c);

      assertThat(prefixMap.getNumKeys(), is(1));

      final Collection<RoutingTableEntry> prior = prefixMap.removeAll(targetPrefix);

      assertThat(prior.size(), is(3));
      assertThat(prior.contains(routingTableEntry1a), is(true));
      assertThat(prior.contains(routingTableEntry1b), is(true));
      assertThat(prior.contains(routingTableEntry1c), is(true));
      assertThat(prefixMap.getEntries(targetPrefix).isEmpty(), is(true));
      assertThat(prefixMap.getNumKeys(), is(0));
    }
  }

  @Test(expected = NullPointerException.class)
  public void testGetRoutingTableEntrysNull() {
    try {
      prefixMap.getEntries(null);
    } catch (NullPointerException npe) {
      assertThat(npe.getMessage(), is("addressPrefix must not be null!"));
      throw npe;
    }
  }

  @Test
  public void testGetRoutingTableEntrysMultipleTimes() {
    final RoutingTableEntry globalRoutingTableEntry = ImmutableRoutingTableEntry.builder()
      .targetPrefix(DEFAULT_TARGET_ADDRESS_PREFIX)
      .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT)
      .build();

    for (int i = 0; i < 10; i++) {
      prefixMap.add(globalRoutingTableEntry);

      final Collection<RoutingTableEntry> routingTableEntrys = prefixMap.getEntries(DEFAULT_TARGET_ADDRESS_PREFIX);

      assertThat("Duplicate RoutingTableEntrys should not be added more than once!", routingTableEntrys.size(), is(1));
    }
  }

  @Test
  public void testGetFilteredRoutingTableEntryMultipleTimes() {
    final RoutingTableEntry globalRoutingTableEntry = ImmutableRoutingTableEntry.builder()
      .targetPrefix(DEFAULT_TARGET_ADDRESS_PREFIX)
      .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT)
      .build();

    final RoutingTableEntry filteredRoutingTableEntry = ImmutableRoutingTableEntry.builder()
      .targetPrefix(DEFAULT_TARGET_ADDRESS_PREFIX)
      .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT)
      .sourcePrefixRestrictionRegex(ACCEPT_NO_SOURCES_PATTERN)
      .build();

    for (int i = 0; i < 10; i++) {
      prefixMap.add(globalRoutingTableEntry);
      prefixMap.add(filteredRoutingTableEntry);

      final Collection<RoutingTableEntry> routingTableEntrys = prefixMap.getEntries(DEFAULT_TARGET_ADDRESS_PREFIX);

      assertThat("Duplicate RoutingTableEntrys should not be added more than once!", routingTableEntrys.size(), is(2));
    }
  }

  @Test
  public void testForEach() {
    final RoutingTableEntry globalRoutingTableEntry = ImmutableRoutingTableEntry.builder()
      .targetPrefix(DEFAULT_TARGET_ADDRESS_PREFIX)
      .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT)
      .build();
    prefixMap.add(globalRoutingTableEntry);

    final RoutingTableEntry globalRoutingTableEntry2 = ImmutableRoutingTableEntry.builder()
      .targetPrefix(DEFAULT_TARGET_ADDRESS_PREFIX.with("foo"))
      .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT)
      .build();
    prefixMap.add(globalRoutingTableEntry2);

    final RoutingTableEntry filteredRoutingTableEntry1 = ImmutableRoutingTableEntry.builder()
      .targetPrefix(DEFAULT_TARGET_ADDRESS_PREFIX)
      .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT)
      .sourcePrefixRestrictionRegex(ACCEPT_NO_SOURCES_PATTERN)
      .build();
    prefixMap.add(filteredRoutingTableEntry1);

    final AtomicInteger atomicInteger = new AtomicInteger();
    prefixMap.forEach((targetAddress, routingTableEntry) -> atomicInteger.getAndIncrement());

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
  public void testGetNextHopRoutingTableEntryForEachPrefix() {
    for (int i = 1; i <= 10; i++) {
      final RoutingTableEntry routingTableEntry = ImmutableRoutingTableEntry.builder()
        .targetPrefix(InterledgerAddressPrefix.of("g." + i))
        .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT)
        .build();
      prefixMap.add(routingTableEntry);

      assertThat(prefixMap.getNumKeys(), is(i));
      final InterledgerAddress destinationAddress = InterledgerAddress.of("g." + i + ".bob");
      assertThat("Each routingTableEntry should be retrieved with only a single value!",
        prefixMap.findNextHops(destinationAddress).size(), is(1)
      );
    }
  }

  @Test
  public void testGetNextHopRoutingTableEntryWithDuplicateDestinations() {
    for (int i = 1; i <= 10; i++) {
      final RoutingTableEntry routingTableEntry = ImmutableRoutingTableEntry.builder()
        .targetPrefix(GLOBAL_ROUTING_TABLE_ENTRY)
        .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT.with("" + i))
        .build();
      prefixMap.add(routingTableEntry);

      final InterledgerAddress destinationAddress = DEFAULT_CONNECTOR_ACCOUNT.with("bob");
      assertThat("Each destination address should map to N number of RoutingTableEntries!",
        prefixMap.findNextHops(destinationAddress).size() > 0, is(true)
      );

      assertThat("The destination address should return the added RoutingTableEntry from the Map!",
        prefixMap.findNextHops(destinationAddress).stream().filter(r -> r.equals(routingTableEntry))
          .collect(Collectors.toList()).size() > 0, is(true)
      );
    }
  }

  @Test
  public void testGetNextHopRoutingTableEntryWithNoRoutingTableEntrysInMap() {
    assertThat(
      prefixMap.findNextHops(DEFAULT_CONNECTOR_ACCOUNT.with("bob")).size(), is(0)
    );
  }

  ////////////////////
  // test getRootPrefix
  ////////////////////

  @Test
  public void testGetNextHopRoutingTableEntryWithNonMatchingDestination1() {
    final RoutingTableEntry routingTableEntry = ImmutableRoutingTableEntry.builder()
      .targetPrefix(GLOBAL_ROUTING_TABLE_ENTRY)
      .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT)
      .build();

    prefixMap.add(routingTableEntry);

    assertThat(prefixMap.findNextHops(InterledgerAddress.of("self.me")).size(), is(0));
    assertThat(prefixMap.findNextHops(InterledgerAddress.of("g.1.me")).size(), is(1));
    assertThat(prefixMap.findNextHops(InterledgerAddress.of("g.1.m")).size(), is(1));
    assertThat(prefixMap.findNextHops(InterledgerAddress.of("g.1")).size(), is(1));
    assertThat(prefixMap.findNextHops(InterledgerAddress.of("g.2")).size(), is(1));
  }

  ////////////////////
  // Private Helpers
  ////////////////////

  @Test
  public void testGetNextHopRoutingTableEntryWithNonMatchingDestination2() {
    final RoutingTableEntry routingTableEntry = ImmutableRoutingTableEntry.builder()
      .targetPrefix(GLOBAL_ROUTING_TABLE_ENTRY.with("foo"))
      .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT)
      .build();

    prefixMap.add(routingTableEntry);

    assertThat(prefixMap.findNextHops(InterledgerAddress.of("self.me")).size(), is(0));
    assertThat(prefixMap.findNextHops(InterledgerAddress.of("g.1.me")).size(), is(0));
    assertThat(prefixMap.findNextHops(InterledgerAddress.of("g.1.m")).size(), is(0));
    assertThat(prefixMap.findNextHops(InterledgerAddress.of("g.1")).size(), is(0));
    assertThat(prefixMap.findNextHops(InterledgerAddress.of("g.2")).size(), is(0));
  }

  @Test
  public void testGetNextHopRoutingTableEntryWithDifferringLengthsInTable() {
    this.prefixMap = this.constructPopulatedPrefixMap();

    assertThat(prefixMap.findNextHops(InterledgerAddress.of("self.me")).size(), is(0));
    assertThat(prefixMap.findNextHops(InterledgerAddress.of("g.1.me")).size(), is(1));
    assertThat(prefixMap.findNextHops(InterledgerAddress.of("g.1.m")).size(), is(1));
    assertThat(prefixMap.findNextHops(InterledgerAddress.of("g.1")).size(), is(1));
    assertThat(prefixMap.findNextHops(InterledgerAddress.of("g.2")).size(), is(1));
    assertThat(prefixMap.findNextHops(InterledgerAddress.of("g.foo.bob")).size(), is(1));
    assertThat(prefixMap.findNextHops(InterledgerAddress.of("g.bar.bob")).size(), is(1));
    assertThat(prefixMap.findNextHops(InterledgerAddress.of("g.baz.boo.alice")).size(), is(1));
    assertThat(prefixMap.findNextHops(InterledgerAddress.of("g.baz.boo.bob")).size(), is(1));
    assertThat(prefixMap.findNextHops(InterledgerAddress.of("g.baz.boo.bar.alice")).size(), is(2));
    assertThat(prefixMap.findNextHops(InterledgerAddress.of("g.baz.boo.bar.bob")).size(), is(2));
  }

  @Test
  public void testGetNextHopRoutingTableEntrysValidateReturnedRoutingTableEntrys() {
    this.prefixMap = this.constructPopulatedPrefixMap();

    final RoutingTableEntry newRoutingTableEntry1 = ImmutableRoutingTableEntry.builder()
      .targetPrefix(GLOBAL_ROUTING_TABLE_ENTRY.with("unittest"))
      .nextHopAccount(InterledgerAddress.of("g.this.account1"))
      .build();
    prefixMap.add(newRoutingTableEntry1);
    Collection<RoutingTableEntry> routingTableEntrys =
      prefixMap.findNextHops(InterledgerAddress.of("g.unittest.receiver"));
    assertThat(routingTableEntrys.contains(newRoutingTableEntry1), is(true));

    final RoutingTableEntry newRoutingTableEntry2 = ImmutableRoutingTableEntry.builder()
      .targetPrefix(GLOBAL_ROUTING_TABLE_ENTRY.with("unittest"))
      .nextHopAccount(InterledgerAddress.of("g.this.account2"))
      .build();
    prefixMap.add(newRoutingTableEntry2);

    routingTableEntrys = prefixMap.findNextHops(InterledgerAddress.of("g.unittest.receiver"));
    assertThat(routingTableEntrys.contains(newRoutingTableEntry1), is(true));
    assertThat(routingTableEntrys.contains(newRoutingTableEntry2), is(true));
  }

  @Test(expected = NullPointerException.class)
  public void testFindLongestPrefixWithNullAddressPrefix() {
    this.prefixMap = constructPopulatedPrefixMap();
    try {
      final InterledgerAddressPrefix nullAddress = null;
      prefixMap.findLongestPrefix(nullAddress);
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("destinationAddressPrefix must not be null!"));
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFindLongestPrefixWithNonPrefix() {
    this.prefixMap = constructPopulatedPrefixMap();
    try {
      prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.foo."));
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(),
        is("An InterledgerAddressPrefix MUST not end with a period (.) character"));
      throw e;
    }
  }

  @Test
  public void testFindLongestPrefix() {
    this.prefixMap = constructPopulatedPrefixMap();

    // g.
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.b")).get().getValue(), is("g"));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.bo")).get().getValue(), is("g"));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.bob")).get().getValue(), is("g"));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.b")).get().getValue(), is("g"));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.f")).get().getValue(), is("g"));
    // contains g.foo, but then some...
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.fool")).get().getValue(), is("g"));

    // g.foo.
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.foo.a")).get().getValue(), is("g.foo"));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.foo.b")).get().getValue(), is("g.foo"));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.foo.bo")).get().getValue(), is("g.foo"));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.foo.alice")).get().getValue(), is("g.foo"));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.foo.bob")).get().getValue(), is("g.foo"));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.foo.foo")).get().getValue(), is("g.foo"));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.foo.bar")).get().getValue(), is("g.foo"));
    // contains g.foo, but then some...
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.foo.fool")).get().getValue(), is("g.foo"));

    // g.bar.
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.bar.a")).get().getValue(), is("g.bar"));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.bar.b")).get().getValue(), is("g.bar"));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.bar.bo")).get().getValue(), is("g.bar"));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.bar.alice")).get().getValue(), is("g.bar"));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.bar.bob")).get().getValue(), is("g.bar"));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.bar.foo")).get().getValue(), is("g.bar"));
    // contains g.bar, but then some...
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.bart")).get().getValue(), is("g"));

    // g.baz.boo.
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.baz.boo.a")).get().getValue(),
      is("g.baz.boo"));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.baz.boo.b")).get().getValue(),
      is("g.baz.boo"));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.baz.boo.bo")).get().getValue(),
      is("g.baz.boo"));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.baz.boo.alice")).get().getValue(),
      is("g.baz.boo"));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.baz.boo.bob")).get().getValue(),
      is("g.baz.boo"));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.baz.boo.foo")).get().getValue(),
      is("g.baz.boo"));
    // contains g.baz, but then some...
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.baz.bool")).get().getValue(), is("g"));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.bazl")).get().getValue(), is("g"));

    // g.baz.boo.bar.
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.baz.boo.bar.a")).get().getValue(),
      is("g.baz.boo.bar"));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.baz.boo.bar.b")).get().getValue(),
      is("g.baz.boo.bar"));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.baz.boo.bar.bo")).get().getValue(),
      is("g.baz.boo.bar"));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.baz.boo.bar.alice")).get().getValue(),
      is("g.baz.boo.bar"));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.baz.boo.bar.bob")).get().getValue(),
      is("g.baz.boo.bar"));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.baz.boo.bar.foo")).get().getValue(),
      is("g.baz.boo.bar"));
    // contains g.baz.boo, but then some...
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.baz.bool.bart")).get().getValue(), is("g"));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.bazl.boo.bar")).get().getValue(), is("g"));

    // g.notfound --> Absent
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.notfound.a")).get().getValue(), is("g"));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.notfound.b")).get().getValue(), is("g"));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.notfound.bo")).get().getValue(), is("g"));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.notfound.alice")).get().getValue(), is("g"));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.notfound.bob")).get().getValue(), is("g"));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.notfound.foo")).get().getValue(), is("g"));

    // g.1. --> g.
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.1.b")).get().getValue(), is("g"));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.1.bo")).get().getValue(), is("g"));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.1.bob")).get().getValue(), is("g"));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.1.b")).get().getValue(), is("g"));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.1.f")).get().getValue(), is("g"));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.1.foo")).get().getValue(), is("g"));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.11")).get().getValue(), is("g"));
    assertThat(prefixMap.findLongestPrefix(InterledgerAddressPrefix.of("g.22")).get().getValue(), is("g"));
  }

  private InterledgerAddressPrefixMap constructPopulatedPrefixMap() {
    final RoutingTableEntry routingTableEntry0 = ImmutableRoutingTableEntry.builder()
      .targetPrefix(ROUTING_TABLE_ENTRY0_TARGET_PREFIX)
      .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT)
      .build();

    final RoutingTableEntry routingTableEntry1 = ImmutableRoutingTableEntry.builder()
      .targetPrefix(ROUTING_TABLE_ENTRY1_TARGET_PREFIX)
      .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT)
      .build();

    final RoutingTableEntry routingTableEntry2 = ImmutableRoutingTableEntry.builder()
      .targetPrefix(ROUTING_TABLE_ENTRY2_TARGET_PREFIX)
      .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT)
      .build();

    final RoutingTableEntry routingTableEntry3 = ImmutableRoutingTableEntry.builder()
      .targetPrefix(ROUTING_TABLE_ENTRY3_TARGET_PREFIX)
      .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT)
      .build();

    final RoutingTableEntry routingTableEntry4 = ImmutableRoutingTableEntry.builder()
      .targetPrefix(ROUTING_TABLE_ENTRY4_TARGET_PREFIX)
      .nextHopAccount(DEFAULT_CONNECTOR_ACCOUNT)
      .build();

    final RoutingTableEntry routingTableEntry5 = ImmutableRoutingTableEntry.builder()
      .targetPrefix(ROUTING_TABLE_ENTRY5_TARGET_PREFIX)
      .nextHopAccount(ROUTING_TABLE_ENTRY5_CONNECTOR_ACCOUNT)
      .build();

    return constructTestPrefixMapWithRoutingTableEntrys(routingTableEntry5, routingTableEntry4, routingTableEntry3,
      routingTableEntry2, routingTableEntry1, routingTableEntry0);
  }

  private InterledgerAddressPrefixMap constructTestPrefixMapWithRoutingTableEntrys(final RoutingTableEntry... routingTableEntry) {
    final InterledgerAddressPrefixMap testMap = new InterledgerAddressPrefixMap();

    for (int i = 0; i < routingTableEntry.length; i++) {
      testMap.add(routingTableEntry[i]);
    }

    return testMap;
  }

}