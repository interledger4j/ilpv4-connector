package com.sappenin.interledger.ilpv4.connector.routing;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit tests for {@link InMemoryRoutingTable}.
 */
public class InMemoryRoutingTableTest {

  // TODO: Restore this!

//  private static final InterledgerAddress TEST1_PREFIX = InterledgerAddress.of("test1.");
//  private static final InterledgerAddress GLOBAL_PREFIX = InterledgerAddress.of("g.");
//
//  private static final InterledgerAddress BOB_GLOBAL_ADDRESS = GLOBAL_PREFIX.with("bank.bob");
//
//  @Mock
//  private InterledgerAddressPrefixMap interledgerPrefixMapMock;
//
//  private InMemoryRoutingTable routingTable;
//
//  @Before
//  public void setup() {
//    MockitoAnnotations.initMocks(this);
//    this.routingTable = new InMemoryRoutingTable(interledgerPrefixMapMock);
//  }
//
//  ////////////////////
//  // Test Constructor
//  ////////////////////
//
//  @Test(expected = NullPointerException.class)
//  public void testNullConstructor() throws Exception {
//    try {
//      new InMemoryRoutingTable(null);
//    } catch (NullPointerException e) {
//      throw e;
//    }
//  }
//
//  ////////////////////
//  // Test testAddRoute
//  ////////////////////
//
//  @Test(expected = NullPointerException.class)
//  public void testAddRouteNull() throws Exception {
//    try {
//      this.routingTable.addRoute(null);
//    } catch (NullPointerException e) {
//      throw e;
//    }
//  }
//
//  @Test
//  public void testAddRoute() throws Exception {
//    final Route getRoute = this.constructTestRoute(GLOBAL_PREFIX);
//
//    this.routingTable.addRoute(getRoute);
//
//    verify(interledgerPrefixMapMock).add(ArgumentMatchers.eq(getRoute));
//    verifyNoMoreInteractions(interledgerPrefixMapMock);
//  }
//
//  ////////////////////
//  // Test testRemoveRoute
//  ////////////////////
//
//  @Test(expected = NullPointerException.class)
//  public void testRemoveRouteNull() throws Exception {
//    try {
//      this.routingTable.removeEntry(null);
//    } catch (NullPointerException e) {
//      throw e;
//    }
//  }
//
//  @Test
//  public void testRemoveRoute() throws Exception {
//    final Route getRoute = this.constructTestRoute(GLOBAL_PREFIX);
//
//    this.routingTable.removeEntry(getRoute);
//
//    verify(interledgerPrefixMapMock).removeEntry(ArgumentMatchers.eq(getRoute));
//    verifyNoMoreInteractions(interledgerPrefixMapMock);
//  }
//
//  ////////////////////
//  // Test removeAllRoutesForPrefix
//  ////////////////////
//
//  @Test(expected = NullPointerException.class)
//  public void testRemoveRoutesNull() throws Exception {
//    try {
//      this.routingTable.removeAllRoutesForPrefix(null);
//    } catch (NullPointerException e) {
//      throw e;
//    }
//  }
//
//  @Test
//  public void testRemoveRoutesWithEmptyTable() throws Exception {
//    final Route getRoute = this.constructTestRoute(GLOBAL_PREFIX);
//
//    this.routingTable.removeAllRoutesForPrefix(getRoute.getRoutePrefix());
//
//    verify(interledgerPrefixMapMock).removeAll(getRoute.getRoutePrefix());
//    verifyNoMoreInteractions(interledgerPrefixMapMock);
//  }
//
//  @Test
//  public void testRemoveRoutesWithNonEmptyTable() throws Exception {
//    final Route route1 = this.constructTestRoute(GLOBAL_PREFIX);
//    final Route route2 = this.constructTestRoute(GLOBAL_PREFIX.with("foo."));
//    this.routingTable.addRoute(route1);
//    this.routingTable.addRoute(route2);
//    when(interledgerPrefixMapMock.removeAll(route1.getRoutePrefix())).thenReturn(ImmutableList.of(route1));
//    verify(interledgerPrefixMapMock).add(route1);
//    verify(interledgerPrefixMapMock).add(route2);
//
//    this.routingTable.removeAllRoutesForPrefix(route1.getRoutePrefix());
//
//    verify(interledgerPrefixMapMock).removeAll(route1.getRoutePrefix());
//    verifyNoMoreInteractions(interledgerPrefixMapMock);
//  }
//
//  @Test
//  public void testRemoveRoutes() throws Exception {
//    final Route getRoute = this.constructTestRoute(GLOBAL_PREFIX);
//    when(interledgerPrefixMapMock.removeAll(getRoute.getRoutePrefix())).thenReturn(ImmutableList.of(getRoute));
//    this.routingTable.addRoute(getRoute);
//    verify(interledgerPrefixMapMock).add(getRoute);
//
//    this.routingTable.removeAllRoutesForPrefix(getRoute.getRoutePrefix());
//
//    verify(interledgerPrefixMapMock).removeAll(getRoute.getRoutePrefix());
//    verifyNoMoreInteractions(interledgerPrefixMapMock);
//  }
//
//  ////////////////////
//  // Test getRouteByPrefix
//  ////////////////////
//
//  @Test(expected = NullPointerException.class)
//  public void testGetRoutesNull() throws Exception {
//    try {
//      this.routingTable.getRouteByPrefix(null);
//    } catch (NullPointerException e) {
//      throw e;
//    }
//  }
//
//  @Test
//  public void testGetRoutes() throws Exception {
//    final Route getRoute = this.constructTestRoute(GLOBAL_PREFIX);
//
//    this.routingTable.getRouteByPrefix(getRoute.getRoutePrefix());
//
//    verify(interledgerPrefixMapMock).getEntries(getRoute.getRoutePrefix());
//    verifyNoMoreInteractions(interledgerPrefixMapMock);
//  }
//
//  ////////////////////
//  // Test forEach
//  ////////////////////
//
//  @Test(expected = NullPointerException.class)
//  public void testForEachNull() throws Exception {
//    try {
//      this.routingTable.forEach(null);
//    } catch (NullPointerException e) {
//      throw e;
//    }
//  }
//
//  @Test
//  public void testForEach() throws Exception {
//    final BiConsumer<String, Collection<Route>> action = (o, o2) -> {
//    };
//
//    this.routingTable.forEach(action);
//
//    verify(interledgerPrefixMapMock).forEach(action);
//    verifyNoMoreInteractions(interledgerPrefixMapMock);
//  }
//
//  ////////////////////
//  // Test findNextHops
//  ////////////////////
//
//  @Test(expected = NullPointerException.class)
//  public void testFindNextHopRouteNull() throws Exception {
//    try {
//      this.routingTable.findNextHops(null);
//    } catch (NullPointerException e) {
//      throw e;
//    }
//  }
//
//  @Test
//  public void testFindNextHopRoute() throws Exception {
//    final Route getRoute = this.constructTestRoute(GLOBAL_PREFIX);
//
//    this.routingTable.findNextHops(BOB_GLOBAL_ADDRESS);
//
//    verify(interledgerPrefixMapMock).findNextHops(BOB_GLOBAL_ADDRESS);
//    verifyNoMoreInteractions(interledgerPrefixMapMock);
//  }
//
//  ////////////////////
//  // Test findNextHopRoutesWithFilter
//  ////////////////////
//
//  @Test(expected = NullPointerException.class)
//  public void testFindNextHopRouteWithFilterNull1() throws Exception {
//    try {
//      InterledgerAddress filter = mock(InterledgerAddress.class);
//      this.routingTable.findNextHops(null, filter);
//    } catch (NullPointerException e) {
//      throw e;
//    }
//  }
//
//  @Test(expected = NullPointerException.class)
//  public void testFindNextHopRouteWithFilterNull2() throws Exception {
//    try {
//      InterledgerAddress filter = mock(InterledgerAddress.class);
//      this.routingTable.findNextHops(filter, null);
//    } catch (NullPointerException e) {
//      throw e;
//    }
//  }
//
//  @Test
//  public void testFindNextHopRouteWithFilter() throws Exception {
//    // All routes allow this sender...
//    final InterledgerAddress usdBarSenderPrefix = GLOBAL_PREFIX.with("usd.bar.");
//    // All routes allow this sender...
//    final InterledgerAddress usdSenderPrefix = GLOBAL_PREFIX.with("usd.");
//    // Depending on the getRoute, this sender is sometimes excluded...
//    final InterledgerAddress cnySenderPrefix = GLOBAL_PREFIX.with("cny.");
//
//    final InterledgerAddress finalDestinationAddress = GLOBAL_PREFIX.with("bank.bob");
//
//    // This getRoute is unrestricted...
//    final Route allSourcesRoute = this.testRouteBuilder(GLOBAL_PREFIX).build();
//    // This getRoute is restricted to all senders whose prefix begins with "g.usd."
//    final Route anyUSDSourcesRoute = this.testRouteBuilder(GLOBAL_PREFIX).sourcePrefixRestrictionRegex(Pattern
//      .compile("g\\.usd\\.(.*)"))
//      .build();
//    // This getRoute is restricted to all senders whose prefix begins with "g.usd.bar."
//    final Route usdBarOnlySourcesRoute = this.testRouteBuilder(GLOBAL_PREFIX).sourcePrefixRestrictionRegex(Pattern
//      .compile("g\\.usd\\.bar\\.(.*)"))
//      .build();
//    // // This getRoute is restricted to all senders...
//    final Route noSourcesRoute = this.testRouteBuilder(GLOBAL_PREFIX).sourcePrefixRestrictionRegex(Pattern
//      .compile("a^"))
//      .build();
//
//    when(interledgerPrefixMapMock.findNextHops(finalDestinationAddress))
//      .thenReturn(
//        Lists.newArrayList(allSourcesRoute, anyUSDSourcesRoute, usdBarOnlySourcesRoute, noSourcesRoute)
//      );
//
//    // The USD Bar Sender is eligible for all routes (except the no-senders getRoute), so we expect 3 to be returned...
//    Collection<Route> actual = this.routingTable.findNextHops(finalDestinationAddress, usdBarSenderPrefix);
//    assertThat(actual.size(), is(3));
//    assertThat(actual.contains(allSourcesRoute), is(true));
//    assertThat(actual.contains(anyUSDSourcesRoute), is(true));
//    assertThat(actual.contains(usdBarOnlySourcesRoute), is(true));
//    assertThat(actual.contains(noSourcesRoute), is(false));
//
//    // The USD Sender is not eligible for the getRoute that requires "g.usd.bar." sources, so we
//    // expect only 2 routes to be returned...
//    actual = this.routingTable.findNextHops(finalDestinationAddress, usdSenderPrefix);
//    assertThat(actual.size(), is(2));
//    assertThat(actual.contains(allSourcesRoute), is(true));
//    assertThat(actual.contains(anyUSDSourcesRoute), is(true));
//    assertThat(actual.contains(usdBarOnlySourcesRoute), is(false));
//    assertThat(actual.contains(noSourcesRoute), is(false));
//
//    // The CNY Sender is eligible for only 1 getRoute, so we expect 1 to be returned...
//    actual = this.routingTable.findNextHops(finalDestinationAddress, cnySenderPrefix);
//    assertThat(actual.size(), is(1));
//    assertThat(actual.contains(allSourcesRoute), is(true));
//    assertThat(actual.contains(anyUSDSourcesRoute), is(false));
//
//    verify(interledgerPrefixMapMock, times(3)).findNextHops(finalDestinationAddress);
//    verifyNoMoreInteractions(interledgerPrefixMapMock);
//  }
//
//  ////////////////////
//  // Private Helpers
//  ////////////////////
//
//  private Route constructTestRoute(final InterledgerAddress targetPrefix) {
//    return ImmutableRoute.builder()
//      .targetPrefix(targetPrefix)
//      .nextHopLedgerAccount(InterledgerAddress.of("g.hub.connie"))
//      .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
//      .build();
//  }
//
//  private ImmutableRoute.Builder testRouteBuilder(final InterledgerAddress targetPrefix) {
//    return ImmutableRoute.builder()
//      .targetPrefix(targetPrefix)
//      .nextHopLedgerAccount(InterledgerAddress.of("g.hub.connie"))
//      .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES));
//  }

}