package com.sappenin.ilpv4.connector.routing;

import com.google.common.collect.ImmutableList;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.core.InterledgerAddress;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

public class SimplePaymentRouterTest {

  private static final InterledgerAddressPrefix BANK_A_PREFIX = InterledgerAddressPrefix.of("test1.banka");
  private static final InterledgerAddress BOB_AT_BANK_B = InterledgerAddress.of("test1.banka.bob");

  @Mock
  private RoutingTable<RoutingTableEntry> routingTableMock;

  private PaymentRouter<RoutingTableEntry> paymentRouter;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    this.paymentRouter = new SimplePaymentRouter(routingTableMock);
  }

  @Test
  public void testFindNextHopRoute() {
    final RoutingTableEntry route1 = mock(RoutingTableEntry.class);
    final RoutingTableEntry route2 = mock(RoutingTableEntry.class);
    when(routingTableMock.findNextHopRoutes(BOB_AT_BANK_B)).thenReturn(ImmutableList.of(route1, route2));

    final Optional<RoutingTableEntry> actual = this.paymentRouter.findBestNexHop(BOB_AT_BANK_B);

    assertThat(actual.isPresent(), is(true));
    for (int i = 0; i < 100; i++) {
      //Try this 100 times to make sure we always get what's expected...
      assertThat(actual.get().equals(route1) || actual.get().equals(route2), is(true));
    }
    verify(routingTableMock).findNextHopRoutes(BOB_AT_BANK_B);
    verifyNoMoreInteractions(routingTableMock);
  }

  @Test
  public void testFindNextHopRouteWithFilter() {
    final RoutingTableEntry route1 = mock(RoutingTableEntry.class);
    final RoutingTableEntry route2 = mock(RoutingTableEntry.class);
    when(routingTableMock.findNextHopRoutes(BOB_AT_BANK_B, BANK_A_PREFIX))
      .thenReturn(ImmutableList.of(route1, route2));

    Optional<RoutingTableEntry> actual = this.paymentRouter.findBestNexHop(BOB_AT_BANK_B, BANK_A_PREFIX);

    assertThat(actual.isPresent(), is(true));
    for (int i = 0; i < 100; i++) {
      //Try this 100 times to make sure we always get what's expected...
      assertThat(actual.get().equals(route1) || actual.get().equals(route2), is(true));
    }
    verify(routingTableMock).findNextHopRoutes(BOB_AT_BANK_B, BANK_A_PREFIX);
    verifyNoMoreInteractions(routingTableMock);
  }

}