package org.interledger.ilpv4.connector.it.blast;

import com.sappenin.interledger.ilpv4.connector.ILPv4Connector;
import com.sappenin.interledger.ilpv4.connector.server.ConnectorServer;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.link.CircuitBreakingLink;
import org.interledger.connector.link.Link;
import org.interledger.connector.link.blast.BlastLink;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.core.InterledgerResponsePacketHandler;
import org.interledger.ilpv4.connector.it.topology.Topology;
import org.slf4j.Logger;

import java.math.BigInteger;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.is;
import static org.interledger.connector.link.PingableLink.PING_PROTOCOL_CONDITION;
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
  protected void testPing(
    final InterledgerAddress senderNodeAddress,
    final AccountId senderAccountId,
    final InterledgerAddress destinationAddress
  )
    throws InterruptedException {

    Objects.requireNonNull(senderNodeAddress);
    Objects.requireNonNull(senderAccountId);
    Objects.requireNonNull(destinationAddress);

    final CountDownLatch latch = new CountDownLatch(1);
    final long start = System.currentTimeMillis();

    final BlastLink blastLink = getBlastLinkFromGraph(senderNodeAddress, senderAccountId);
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
   * Helper method to obtain an instance of {@link ILPv4Connector} from the topology, based upon its
   * Interledger Address.
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
   * Helper method to obtain an instance of {@link Link} from the topology, based upon its Interledger Address.
   *
   * @param nodeAddress The {@link InterledgerAddress} of the node in the graph to obtain a Link from.
   * @param accountId   The unique account identifier for the Link to return.*
   *
   * @return A {@link BlastLink} in the Topology that corresponds to the supplied inputs.
   */
  protected BlastLink getBlastLinkFromGraph(final InterledgerAddress nodeAddress, final AccountId accountId) {
    Objects.requireNonNull(nodeAddress);
    Objects.requireNonNull(accountId);

    final Link link = getILPv4NodeFromGraph(nodeAddress).getLinkManager()
      .getOrCreateLink(accountId);

    if (BlastLink.LINK_TYPE.equals(link.getLinkSettings().getLinkType())) {
      // Most of the time, this link is a CircuitBreaking link, in which case the BlastLink is the Delegate.
      if (CircuitBreakingLink.class.isAssignableFrom(link.getClass())) {
        return ((CircuitBreakingLink) link).getLinkDelegateTyped();
      } else {
        return (BlastLink) link;
      }
    } else {
      throw new RuntimeException(
        "Link was not of Type(BLAST), but was instead: " + link.getLinkSettings().getLinkType());
    }
  }

  protected void assertAccountBalance(
    final ILPv4Connector connector,
    final AccountId accountId,
    final BigInteger expectedAmount
  ) {
    assertThat(
      String.format("Incorrect balance for `%s` @ `%s`!", accountId, connector.getNodeIlpAddress().get().getValue()),
      connector.getBalanceTracker().getBalance(accountId).getAmount().get(), is(expectedAmount.intValue())
    );
  }
}
