package com.sappenin.interledger.ilpv4.connector.routing;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

// TODO: Delete this, or transition to RoutingServiceTest.
public class SimplePaymentRouterTest {

//  private static final InterledgerAddressPrefix BANK_A_PREFIX = InterledgerAddressPrefix.of("test1.banka");
//  private static final InterledgerAddress BOB_AT_BANK_B = InterledgerAddress.of("test1.banka.bob");
//
//  @Mock
//  private RoutingTable<Route> routingTableMock;
//
//  private PaymentRouter<Route> paymentRouter;
//
//  @Before
//  public void setup() {
//    MockitoAnnotations.initMocks(this);
//    this.paymentRouter = new SimplePaymentRouter(routingTableMock);
//  }
//
//  @Test
//  public void testFindNextHopRoute() {
//    final Route route1 = mock(Route.class);
//    final Route route2 = mock(Route.class);
//    when(routingTableMock.findNextHopRoute(BOB_AT_BANK_B)).thenReturn(Optional.of(route1));
//
//    final Optional<Route> actual = this.paymentRouter.findBestNexHop(BOB_AT_BANK_B);
//
//    assertThat(actual.isPresent(), is(true));
//    for (int i = 0; i < 100; i++) {
//      //Try this 100 times to make sure we always getEntry what's expected...
//      assertThat(actual.get().equals(route1) || actual.get().equals(route2), is(true));
//    }
//    verify(routingTableMock).findNextHopRoute(BOB_AT_BANK_B);
//    verifyNoMoreInteractions(routingTableMock);
//  }
//
//  //  @Test
//  //  public void testFindNextHopRouteWithFilter() {
//  //    final Route route1 = mock(Route.class);
//  //    final Route route2 = mock(Route.class);
//  //    when(routingTableMock.findNextHopRoute(BOB_AT_BANK_B, BANK_A_PREFIX))
//  //      .thenReturn(ImmutableList.of(route1, route2));
//  //
//  //    Optional<Route> actual = this.paymentRouter.findBestNexHop(BOB_AT_BANK_B, BANK_A_PREFIX);
//  //
//  //    assertThat(actual.isPresent(), is(true));
//  //    for (int i = 0; i < 100; i++) {
//  //      //Try this 100 times to make sure we always getEntry what's expected...
//  //      assertThat(actual.getEntry().equals(route1) || actual.getEntry().equals(route2), is(true));
//  //    }
//  //    verify(routingTableMock).findNextHopRoute(BOB_AT_BANK_B, BANK_A_PREFIX);
//  //    verifyNoMoreInteractions(routingTableMock);
//  //  }

}