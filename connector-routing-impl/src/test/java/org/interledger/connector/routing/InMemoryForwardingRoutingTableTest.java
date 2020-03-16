package org.interledger.connector.routing;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.crypto.ByteArrayUtils;

import org.junit.Before;
import org.junit.Test;

import java.time.Instant;

public class InMemoryForwardingRoutingTableTest {

  private static final InterledgerAddressPrefix BOB_PREFIX = InterledgerAddressPrefix.of("test.node.bob");
  private static final InterledgerAddressPrefix ALICE_PREFIX = InterledgerAddressPrefix.of("test.node.alice");
  private static final AccountId BOB_ACCT = AccountId.of("Bob");
  private static final AccountId ALICE_ACCT = AccountId.of("Alice");

  private InMemoryForwardingRoutingTable routingTable;

  @Before
  public void setUp() {
    routingTable = new InMemoryForwardingRoutingTable();
  }

  @Test
  public void clearRouteInLogAtEpoch() {
    assertThat(routingTable.getCurrentEpoch()).isEqualTo(0);
    routingTable.setEpochValue(1, createRouteUpdate(1, BOB_ACCT, BOB_PREFIX));
    assertThat(routingTable.getCurrentEpoch()).isEqualTo(1);
    ImmutableRouteUpdate aliceRouteUpdate = createRouteUpdate(1, ALICE_ACCT, ALICE_PREFIX);
    routingTable.setEpochValue(2, aliceRouteUpdate);
    routingTable.clearRouteInLogAtEpoch(1);

    assertThat(routingTable.getPartialRouteLog(0, 10)).hasSize(2)
      .containsExactlyInAnyOrder(null, aliceRouteUpdate);
  }

  @Test
  public void setEpochValue() {
    assertThat(routingTable.getCurrentEpoch()).isEqualTo(0);
    routingTable.setEpochValue(1, createRouteUpdate(1, BOB_ACCT, BOB_PREFIX));
    assertThat(routingTable.getCurrentEpoch()).isEqualTo(1);
    routingTable.setEpochValue(2, createRouteUpdate(1, ALICE_ACCT, ALICE_PREFIX));
    assertThat(routingTable.getCurrentEpoch()).isEqualTo(2);
  }

  @Test
  public void getPartialRouteLog() {
    ImmutableRouteUpdate bobRouteUpdate = createRouteUpdate(1, BOB_ACCT, BOB_PREFIX);
    ImmutableRouteUpdate aliceRouteUpdate = createRouteUpdate(1, ALICE_ACCT, ALICE_PREFIX);

    routingTable.setEpochValue(1, bobRouteUpdate);
    routingTable.setEpochValue(2, aliceRouteUpdate);

    assertThat(routingTable.getPartialRouteLog(0, 10)).hasSize(2)
      .containsExactlyInAnyOrder(bobRouteUpdate, aliceRouteUpdate);

    assertThat(routingTable.getPartialRouteLog(1, 10)).hasSize(1)
      .containsExactlyInAnyOrder(aliceRouteUpdate);

    assertThat(routingTable.getPartialRouteLog(2, 10)).isEmpty();
  }

  /**
   * Tests the unepected behavior of setEpochValue being called with an older epoch value. The expected behavior
   * is unclear but currently honors old values.
   */
  @Test
  public void setEpochOldValue() {
    assertThat(routingTable.getCurrentEpoch()).isEqualTo(0);
    routingTable.setEpochValue(1, createRouteUpdate(1, BOB_ACCT, BOB_PREFIX));
    assertThat(routingTable.getCurrentEpoch()).isEqualTo(1);
    routingTable.setEpochValue(0, createRouteUpdate(0, ALICE_ACCT, ALICE_PREFIX));
    assertThat(routingTable.getCurrentEpoch()).isEqualTo(0);
  }

  private ImmutableRouteUpdate createRouteUpdate(int epoch, AccountId nextHop, InterledgerAddressPrefix prefix) {
    return ImmutableRouteUpdate.builder().route(
      Route.builder().nextHopAccountId(nextHop)
        .routePrefix(prefix)
        .expiresAt(Instant.now().plusSeconds(epoch))
        .auth(ByteArrayUtils.generate32RandomBytes())
        .build())
      .routePrefix(prefix)
      .epoch(1)
      .build();
  }


}