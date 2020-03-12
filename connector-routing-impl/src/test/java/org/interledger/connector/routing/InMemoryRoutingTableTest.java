package org.interledger.connector.routing;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.function.BiConsumer;

/**
 * Unit tests for {@link InMemoryRoutingTable}.
 */
public class InMemoryRoutingTableTest {


  private static final InterledgerAddressPrefix GLOBAL_PREFIX = InterledgerAddressPrefix.of("g");

  private static final InterledgerAddress BOB_GLOBAL_ADDRESS = InterledgerAddress.of("g.bank.bob");

  @Mock
  private InterledgerAddressPrefixMap interledgerPrefixMapMock;

  private InMemoryRoutingTable routingTable;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    this.routingTable = new InMemoryRoutingTable(interledgerPrefixMapMock);
  }

  ////////////////////
  // Test Constructor
  ////////////////////

  @Test(expected = NullPointerException.class)
  public void testNullConstructor() {
    try {
      new InMemoryRoutingTable(null);
    } catch (NullPointerException e) {
      throw e;
    }
  }

  ////////////////////
  // Test testAddRoute
  ////////////////////

  @Test(expected = NullPointerException.class)
  public void testAddRouteNull() {
    try {
      this.routingTable.addRoute(null);
    } catch (NullPointerException e) {
      throw e;
    }
  }

  @Test
  public void testAddRoute() {
    final Route route = this.constructTestRoute(GLOBAL_PREFIX);

    this.routingTable.addRoute(route);

    verify(interledgerPrefixMapMock).putEntry(route.routePrefix(), route);
    verifyNoMoreInteractions(interledgerPrefixMapMock);
  }

  ////////////////////
  // Test testRemoveRoute
  ////////////////////

  @Test(expected = NullPointerException.class)
  public void testRemoveRouteNull() {
    try {
      this.routingTable.removeRoute(null);
    } catch (NullPointerException e) {
      throw e;
    }
  }

  @Test
  public void testRemoveRoute() {
    final Route route = this.constructTestRoute(GLOBAL_PREFIX);

    this.routingTable.removeRoute(route.routePrefix());

    verify(interledgerPrefixMapMock).removeEntry(route.routePrefix());
    verifyNoMoreInteractions(interledgerPrefixMapMock);
  }

  ////////////////////
  // Test getRouteByPrefix
  ////////////////////

  @Test(expected = NullPointerException.class)
  public void testGetRoutesNull() {
    try {
      this.routingTable.getRouteByPrefix(null);
    } catch (NullPointerException e) {
      throw e;
    }
  }

  @Test
  public void testGetRoutes() {
    final Route route = this.constructTestRoute(GLOBAL_PREFIX);

    this.routingTable.getRouteByPrefix(route.routePrefix());

    verify(interledgerPrefixMapMock).getEntry(route.routePrefix());
    verifyNoMoreInteractions(interledgerPrefixMapMock);
  }

  ////////////////////
  // Test forEach
  ////////////////////

  @Test(expected = NullPointerException.class)
  public void testForEachNull() {
    try {
      this.routingTable.forEach(null);
    } catch (NullPointerException e) {
      throw e;
    }
  }

  @Test
  public void testForEach() {
    final BiConsumer<String, Collection<Route>> action = (o, o2) -> {
    };

    this.routingTable.forEach(action);

    verify(interledgerPrefixMapMock).forEach(action);
    verifyNoMoreInteractions(interledgerPrefixMapMock);
  }

  ////////////////////
  // Test findNextHops
  ////////////////////

  @Test(expected = NullPointerException.class)
  public void testFindNextHopRouteNull() {
    try {
      this.routingTable.findNextHopRoute(null);
    } catch (NullPointerException e) {
      throw e;
    }
  }

  @Test
  public void testFindNextHopRoute() {
    this.routingTable.findNextHopRoute(BOB_GLOBAL_ADDRESS);

    verify(interledgerPrefixMapMock).findNextHop(BOB_GLOBAL_ADDRESS);
    verifyNoMoreInteractions(interledgerPrefixMapMock);
  }

  ////////////////////
  // Private Helpers
  ////////////////////

  private Route constructTestRoute(final InterledgerAddressPrefix targetPrefix) {
    return ImmutableRoute.builder()
      .routePrefix(targetPrefix)
      .nextHopAccountId(AccountId.of("connie"))
      .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
      .build();
  }

}
