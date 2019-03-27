package org.interledger.ilpv4.connector.it.blast;

import com.sappenin.interledger.ilpv4.connector.ILPv4Connector;
import com.sappenin.interledger.ilpv4.connector.server.ConnectorServer;
import org.interledger.connector.accounts.Account;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.link.CircuitBreakingLink;
import org.interledger.connector.link.blast.BlastLink;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.core.InterledgerResponsePacketHandler;
import org.interledger.ilpv4.connector.it.topology.LinkNode;
import org.interledger.ilpv4.connector.it.topology.Topology;
import org.slf4j.Logger;

import java.math.BigInteger;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.is;
import static org.interledger.connector.link.PingableLink.PING_PROTOCOL_CONDITION;
import static org.interledger.ilpv4.connector.it.topologies.blast.TwoConnectorPeerBlastTopology.ALICE_ADDRESS;
import static org.junit.Assert.assertThat;

/**
 * Abstract parent class for BLAST Integration tests.
 */
public abstract class AbstractBlastIT {

  protected abstract Logger getLogger();

  protected abstract Topology getTopology();

  /**
   * Helper method to testing ping functionality.
   *
   * @param senderNodeAddress  The {@link InterledgerAddress} for the node initiating the ILP ping.
   * @param destinationAddress The {@link InterledgerAddress} to ping.
   */
  protected void testPing(final InterledgerAddress senderNodeAddress, final InterledgerAddress destinationAddress)
    throws InterruptedException {

    Objects.requireNonNull(senderNodeAddress);
    Objects.requireNonNull(destinationAddress);

    final CountDownLatch latch = new CountDownLatch(1);
    final long start = System.currentTimeMillis();

    final BlastLink blastLink = getBlastLinkFromGraph(ALICE_ADDRESS);
    final InterledgerResponsePacket responsePacket = blastLink.ping(destinationAddress, BigInteger.ONE);

    new InterledgerResponsePacketHandler() {
      @Override
      protected void handleFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
        assertThat(interledgerFulfillPacket.getFulfillment().validateCondition(PING_PROTOCOL_CONDITION), is(true));
        latch.countDown();
      }

      @Override
      protected void handleRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
        fail(String.format("Ping request rejected, but should have fulfilled: %s", interledgerRejectPacket));
        latch.countDown();
      }

    }.handle(responsePacket);

    latch.await(5, TimeUnit.SECONDS);
    final long end = System.currentTimeMillis();
    getLogger().info("Ping took {}ms", end - start);
  }

  /**
   * Helper method to obtain an instance of {@link ILPv4Connector} from the topology, based upon its Interledger
   * Address.
   *
   * @param interledgerAddress
   *
   * @return
   */
  protected ILPv4Connector getILPv4NodeFromGraph(final InterledgerAddress interledgerAddress) {
    Objects.requireNonNull(interledgerAddress);
    return ((ConnectorServer) getTopology().getNode(interledgerAddress.getValue()).getContentObject()).getContext()
      .getBean(ILPv4Connector.class);
  }

  /**
   * Helper method to obtain an instance of {@link LinkNode} from the topology, based upon its Interledger Address.
   *
   * @param interledgerAddress The unique key of the node to return.
   *
   * @return
   */
  protected BlastLink getBlastLinkFromGraph(final InterledgerAddress interledgerAddress) {
    Objects.requireNonNull(interledgerAddress);
    final CircuitBreakingLink circuitBreakingLink =
      (CircuitBreakingLink) getILPv4NodeFromGraph(interledgerAddress).getAccountManager().getAllAccounts()
        .filter(account -> account.getAccountSettings().getLinkType().equals(BlastLink.LINK_TYPE))
        .findFirst()
        .map(Account::getLink)
        .get();
    return (BlastLink) circuitBreakingLink.getLinkDelegate();
  }

  protected void assertAccountBalance(
    final ILPv4Connector connector,
    final AccountId accountId,
    final BigInteger expectedAmount
  ) {
    assertThat(
      String.format("Incorrect balance for `%s@%s`!", accountId, connector.getNodeIlpAddress().get().getValue()),
      connector.getBalanceTracker().getBalance(accountId).getAmount().get(), is(expectedAmount.intValue())
    );
  }
}
