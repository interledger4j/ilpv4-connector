package com.sappenin.ilpv4.connector.routing2;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.interledger.core.InterledgerAddressPrefix;
import com.sappenin.ilpv4.connector.routing.ImmutableRoutingTableEntry;
import com.sappenin.ilpv4.connector.routing.InMemoryRoutingTable;
import com.sappenin.ilpv4.connector.routing.InterledgerAddressPrefixMap;
import com.sappenin.ilpv4.connector.routing.RoutingTableEntry;
import org.interledger.core.InterledgerAddress;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link InMemoryRoutingTable}.
 */
public class InMemoryRoutingTableTest {

  private static final InterledgerAddressPrefix GLOBAL_PREFIX = InterledgerAddressPrefix.of("g");
  private static final InterledgerAddress BOB_GLOBAL_ADDRESS = InterledgerAddress.builder().value("g.bank.bob").build();

  private static final InterledgerAddressPrefix BOB_GLOBAL_ADDRESS_PREFIX =
    InterledgerAddressPrefix.builder().value("g.bank.bob").build();

  @Mock
  private InterledgerAddressPrefixMap<RoutingTableEntry> prefixMap;

  private InMemoryRoutingTable routingTable;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    this.routingTable = new InMemoryRoutingTable(prefixMap);
  }

  ////////////////////
  // Test Constructor
  ////////////////////

  @Test(expected = NullPointerException.class)
  public void testNullConstructor() {
    try {
      new InMemoryRoutingTable(null);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is(nullValue()));
      throw e;
    }
  }

  ////////////////////
  // Test testAddRoutingTableEntry
  ////////////////////

  @Test(expected = NullPointerException.class)
  public void testAddRoutingTableEntryNull() {
    try {
      this.routingTable.addRoute(null);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is(nullValue()));
      throw e;
    }
  }

  @Test
  public void testAddRoutingTableEntry() {
    final RoutingTableEntry routingTableEntry = this.constructTestRoutingTableEntry(BOB_GLOBAL_ADDRESS_PREFIX);

    this.routingTable.addRoute(routingTableEntry);

    verify(prefixMap).add(ArgumentMatchers.eq(routingTableEntry));
    verifyNoMoreInteractions(prefixMap);
  }

  ////////////////////
  // Test testRemoveRoutingTableEntry
  ////////////////////

  @Test(expected = NullPointerException.class)
  public void testRemoveRoutingTableEntryNull() {
    try {
      this.routingTable.removeRoute(null);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is(nullValue()));
      throw e;
    }
  }

  @Test
  public void testRemoveRoutingTableEntry() {
    final RoutingTableEntry routingTableEntry = this.constructTestRoutingTableEntry(BOB_GLOBAL_ADDRESS_PREFIX);

    this.routingTable.removeRoute(routingTableEntry);

    verify(prefixMap).remove(ArgumentMatchers.eq(routingTableEntry));
    verifyNoMoreInteractions(prefixMap);
  }

  ////////////////////
  // Test removeAllRoutingTableEntrysForTargetPrefix
  ////////////////////

  @Test(expected = NullPointerException.class)
  public void testRemoveRoutingTableEntrysNull() {
    try {
      this.routingTable.removeAllRoutesForTargetPrefix(null);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is(nullValue()));
      throw e;
    }
  }

  @Test
  public void testRemoveRoutingTableEntrysWithEmptyTable() {
    //final RoutingTableEntry routingTableEntry = this.constructTestRoutingTableEntry(BOB_GLOBAL_ADDRESS_PREFIX);

    this.routingTable.removeAllRoutesForTargetPrefix(BOB_GLOBAL_ADDRESS_PREFIX);

    verify(prefixMap).removeAll(BOB_GLOBAL_ADDRESS_PREFIX);
    verifyNoMoreInteractions(prefixMap);
  }

  @Test
  public void testRemoveRoutingTableEntrysWithNonEmptyTable() {

    final RoutingTableEntry routingTableEntry1 = this.constructTestRoutingTableEntry(BOB_GLOBAL_ADDRESS_PREFIX);
    final RoutingTableEntry routingTableEntry2 =
      this.constructTestRoutingTableEntry(BOB_GLOBAL_ADDRESS_PREFIX.with("foo"));
    this.routingTable.addRoute(routingTableEntry1);
    this.routingTable.addRoute(routingTableEntry2);

    when(prefixMap.removeAll(BOB_GLOBAL_ADDRESS_PREFIX)).thenReturn(ImmutableList.of(routingTableEntry1));

    verify(prefixMap).add(routingTableEntry1);
    verify(prefixMap).add(routingTableEntry2);

    this.routingTable.removeAllRoutesForTargetPrefix(BOB_GLOBAL_ADDRESS_PREFIX);

    verify(prefixMap).removeAll(BOB_GLOBAL_ADDRESS_PREFIX);
    verifyNoMoreInteractions(prefixMap);
  }

  @Test
  public void testRemoveRoutingTableEntrys() {
    final RoutingTableEntry routingTableEntry = this.constructTestRoutingTableEntry(BOB_GLOBAL_ADDRESS_PREFIX);
    when(prefixMap.removeAll(BOB_GLOBAL_ADDRESS_PREFIX)).thenReturn(ImmutableList.of(routingTableEntry));
    this.routingTable.addRoute(routingTableEntry);
    verify(prefixMap).add(routingTableEntry);

    this.routingTable.removeAllRoutesForTargetPrefix(BOB_GLOBAL_ADDRESS_PREFIX);

    verify(prefixMap).removeAll(BOB_GLOBAL_ADDRESS_PREFIX);
    verifyNoMoreInteractions(prefixMap);
  }

  ////////////////////
  // Test getRoutingTableEntrysByTargetPrefix
  ////////////////////

  @Test(expected = NullPointerException.class)
  public void testGetRoutingTableEntrysNull() {
    try {
      this.routingTable.getRoutesByTargetPrefix(null);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is(nullValue()));
      throw e;
    }
  }

  @Test
  public void testGetRoutingTableEntrys() {
    this.routingTable.getRoutesByTargetPrefix(BOB_GLOBAL_ADDRESS_PREFIX);

    verify(prefixMap).getEntries(BOB_GLOBAL_ADDRESS_PREFIX);
    verifyNoMoreInteractions(prefixMap);
  }

  ////////////////////
  // Test forEach
  ////////////////////

  @Test(expected = NullPointerException.class)
  public void testForEachNull() {
    try {
      this.routingTable.forEach(null);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is(nullValue()));
      throw e;
    }
  }

  @Test
  public void testForEach() {
    final BiConsumer<String, Collection<RoutingTableEntry>> action = (o, o2) -> {
    };

    this.routingTable.forEach(action);

    verify(prefixMap).forEach(action);
    verifyNoMoreInteractions(prefixMap);
  }

  ////////////////////
  // Test findNextHopRoutes
  ////////////////////

  @Test(expected = NullPointerException.class)
  public void testFindNextHopRoutingTableEntryNull() {
    try {
      this.routingTable.findNextHopRoutes(null);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("finalDestinationAddress must not be null!"));
      throw e;
    }
  }

  @Test
  public void testFindNextHopRoutingTableEntry() {
    this.routingTable.findNextHopRoutes(BOB_GLOBAL_ADDRESS);
    verify(prefixMap).findNextHops(BOB_GLOBAL_ADDRESS);
    verifyNoMoreInteractions(prefixMap);
  }

  ////////////////////
  // Test findNextHopRoutingTableEntrysWithFilter
  ////////////////////

  @Test(expected = NullPointerException.class)
  public void testFindNextHopRoutingTableEntryWithFilterNull1() {
    try {
      InterledgerAddressPrefix filter = mock(InterledgerAddressPrefix.class);
      this.routingTable.findNextHopRoutes(null, filter);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is(nullValue()));
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void testFindNextHopRoutingTableEntryWithFilterNull2() {
    try {
      InterledgerAddress filter = mock(InterledgerAddress.class);
      this.routingTable.findNextHopRoutes(filter, null);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is(nullValue()));
      throw e;
    }
  }

  @Test
  public void testFindNextHopRoutingTableEntryWithFilter() {
    // All routingTableEntrys allow this sender...
    final InterledgerAddressPrefix usdBarSenderPrefix = GLOBAL_PREFIX.with("usd.bar");
    // All routingTableEntrys allow this sender...
    final InterledgerAddressPrefix usdSenderPrefix = GLOBAL_PREFIX.with("usd");
    // Depending on the routingTableEntry, this sender is sometimes excluded...
    final InterledgerAddressPrefix cnySenderPrefix = GLOBAL_PREFIX.with("cny");

    final InterledgerAddress finalDestinationAddress = InterledgerAddress.of("g.bank.bob");

    // This routingTableEntry is unrestricted...
    final RoutingTableEntry allSourcesRoutingTableEntry =
      this.testRoutingTableEntryBuilder(BOB_GLOBAL_ADDRESS_PREFIX).build();
    // This routingTableEntry is restricted to all senders whose prefix begins with "g.usd"
    final RoutingTableEntry anyUSDSourcesRoutingTableEntry =
      this.testRoutingTableEntryBuilder(BOB_GLOBAL_ADDRESS_PREFIX)
        .sourcePrefixRestrictionRegex(Pattern.compile("g\\.usd(.*)"))
        .build();
    // This routingTableEntry is restricted to all senders whose prefix begins with "g.usd.bar"
    final RoutingTableEntry usdBarOnlySourcesRoutingTableEntry =
      this.testRoutingTableEntryBuilder(BOB_GLOBAL_ADDRESS_PREFIX)
        .sourcePrefixRestrictionRegex(Pattern.compile("g\\.usd\\.bar(.*)"))
        .build();
    // // This routingTableEntry is restricted to all senders...
    final RoutingTableEntry noSourcesRoutingTableEntry =
      this.testRoutingTableEntryBuilder(BOB_GLOBAL_ADDRESS_PREFIX).sourcePrefixRestrictionRegex(Pattern
        .compile("a^"))
        .build();

    when(prefixMap.findNextHops(finalDestinationAddress))
      .thenReturn(
        Lists.newArrayList(
          allSourcesRoutingTableEntry, anyUSDSourcesRoutingTableEntry, usdBarOnlySourcesRoutingTableEntry,
          noSourcesRoutingTableEntry
        )
      );

    // The USD Bar Sender is eligible for all routingTableEntries (except the no-senders routingTableEntry), so we
    // expect 3 to be returned...
    Collection<RoutingTableEntry> actual =
      this.routingTable.findNextHopRoutes(finalDestinationAddress, usdBarSenderPrefix);
    assertThat(actual.size(), is(3));
    assertThat(actual.contains(allSourcesRoutingTableEntry), is(true));
    assertThat(actual.contains(anyUSDSourcesRoutingTableEntry), is(true));
    assertThat(actual.contains(usdBarOnlySourcesRoutingTableEntry), is(true));
    assertThat(actual.contains(noSourcesRoutingTableEntry), is(false));

    // The USD Sender is not eligible for the routingTableEntry that requires "g.usd.bar." sources, so we
    // expect only 2 routingTableEntrys to be returned...
    actual = this.routingTable.findNextHopRoutes(finalDestinationAddress, usdSenderPrefix);
    assertThat(actual.size(), is(2));
    assertThat(actual.contains(allSourcesRoutingTableEntry), is(true));
    assertThat(actual.contains(anyUSDSourcesRoutingTableEntry), is(true));
    assertThat(actual.contains(usdBarOnlySourcesRoutingTableEntry), is(false));
    assertThat(actual.contains(noSourcesRoutingTableEntry), is(false));

    // The CNY Sender is eligible for only 1 routingTableEntry, so we expect 1 to be returned...
    actual = this.routingTable.findNextHopRoutes(finalDestinationAddress, cnySenderPrefix);
    assertThat(actual.size(), is(1));
    assertThat(actual.contains(allSourcesRoutingTableEntry), is(true));
    assertThat(actual.contains(anyUSDSourcesRoutingTableEntry), is(false));

    verify(prefixMap, times(3)).findNextHops(finalDestinationAddress);
    verifyNoMoreInteractions(prefixMap);
  }

  ////////////////////
  // Private Helpers
  ////////////////////

  private RoutingTableEntry constructTestRoutingTableEntry(
    final InterledgerAddressPrefix targetPrefix
  ) {
    return ImmutableRoutingTableEntry.builder()
      .targetPrefix(targetPrefix)
      .nextHopAccount(InterledgerAddress.of("g.hub.connie"))
      .build();
  }

  private ImmutableRoutingTableEntry.Builder testRoutingTableEntryBuilder(final InterledgerAddressPrefix targetPrefix) {
    return ImmutableRoutingTableEntry.builder()
      .targetPrefix(targetPrefix)
      .nextHopAccount(InterledgerAddress.of("g.hub.connie"))
      .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES));
  }

}