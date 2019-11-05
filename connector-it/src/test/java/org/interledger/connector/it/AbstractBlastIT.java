package org.interledger.connector.it;

import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.is;
import static org.interledger.connector.config.BalanceTrackerConfig.BALANCE_TRACKING_JACKSON_REDIS_TEMPLATE_BEAN_NAME;
import static org.interledger.connector.core.ConfigConstants.ADMIN_PASSWORD;
import static org.interledger.connector.core.ConfigConstants.DEFAULT_JWT_TOKEN_ISSUER;
import static org.interledger.connector.core.ConfigConstants.DOT;
import static org.interledger.connector.core.ConfigConstants.INTERLEDGER__CONNECTOR__GLOBAL_ROUTING_SETTINGS__ROUTING_SECRET;
import static org.interledger.connector.core.ConfigConstants.INTERLEDGER__CONNECTOR__KEYSTORE__JKS__ENABLED;
import static org.interledger.connector.core.ConfigConstants.INTERLEDGER__CONNECTOR__KEYSTORE__JKS__FILENAME;
import static org.interledger.connector.core.ConfigConstants.INTERLEDGER__CONNECTOR__KEYSTORE__JKS__PASSWORD;
import static org.interledger.connector.core.ConfigConstants.INTERLEDGER__CONNECTOR__KEYSTORE__JKS__SECRET0_ALIAS;
import static org.interledger.connector.core.ConfigConstants.INTERLEDGER__CONNECTOR__KEYSTORE__JKS__SECRET0_PASSWORD;
import static org.interledger.connector.it.topologies.AbstractTopology.ALICE;
import static org.interledger.connector.it.topologies.AbstractTopology.BOB;
import static org.interledger.connector.it.topologies.AbstractTopology.PAUL;
import static org.interledger.connector.it.topologies.AbstractTopology.PETER;
import static org.interledger.connector.routing.PaymentRouter.PING_ACCOUNT_ID;
import static org.junit.Assert.assertThat;

import org.interledger.connector.ILPv4Connector;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.balances.BalanceTracker;
import org.interledger.connector.balances.InMemoryBalanceTracker;
import org.interledger.connector.balances.RedisBalanceTracker;
import org.interledger.connector.core.ConfigConstants;
import org.interledger.connector.it.topology.Node;
import org.interledger.connector.it.topology.Topology;
import org.interledger.connector.it.topology.nodes.ConnectorServerNode;
import org.interledger.connector.link.CircuitBreakingLink;
import org.interledger.connector.ping.DefaultPingInitiator;
import org.interledger.connector.ping.PingInitiator;
import org.interledger.connector.server.ConnectorServer;
import org.interledger.connector.server.spring.settings.javamoney.SpringServiceProvider;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.link.Link;
import org.interledger.link.PingLoopbackLink;
import org.interledger.link.exceptions.LinkException;
import org.interledger.link.http.IlpOverHttpLink;

import com.google.common.primitives.UnsignedLong;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisKeyCommands;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.money.spi.Bootstrap;

/**
 * Abstract parent class for BLAST Integration tests.
 */
public abstract class AbstractBlastIT {

  @BeforeClass
  public static void setupClass() {
    // For whatever reason, the JavaMoney Bootstrap mechanism doesn't pickup the service provider directive in
    // `src/main/resources/META-INF/services/javax.money.spi.ServiceProvider` in the `connector-server` module.
    // Thus, we need to programmatically wire-in JavaMoney into Spring using this manual call (before any JavaMoney
    // code is engaged).
    Bootstrap.init(new SpringServiceProvider());

    System.setProperty("spring.jpa.properties.hibernate.temp.use_jdbc_metadata_defaults", "false");

    System.setProperty(DEFAULT_JWT_TOKEN_ISSUER, "https://connie.example.com");
    System.setProperty(ADMIN_PASSWORD, "password");

    // Configure JKS Properly for test purposes
    // For dev/test purposes this is fine, but not for real use-cases. Use KMS instead.
    System.setProperty(INTERLEDGER__CONNECTOR__KEYSTORE__JKS__ENABLED, "true");
    System.setProperty(INTERLEDGER__CONNECTOR__KEYSTORE__JKS__FILENAME, "crypto/crypto.p12");
    System.setProperty(INTERLEDGER__CONNECTOR__KEYSTORE__JKS__PASSWORD, "password");
    // For dev/test purposes this is fine, but not for real use-cases. Encrypt this value instead.
    System.setProperty(INTERLEDGER__CONNECTOR__KEYSTORE__JKS__SECRET0_ALIAS, "secret0");
    System.setProperty(INTERLEDGER__CONNECTOR__KEYSTORE__JKS__SECRET0_PASSWORD, "password");
    //A simulated routing secret, which is a seed used for generating routing table auth values. Represents the
    // plaintext value of `shh`, encrypted.
    System.setProperty(INTERLEDGER__CONNECTOR__GLOBAL_ROUTING_SETTINGS__ROUTING_SECRET,
      "enc:JKS:crypto.p12:secret0:1:aes_gcm:AAAADKZPmASojt1iayb2bPy4D-Toq7TGLTN95HzCQAeJtz0=");

    // Required to get the conditional-config to work for this topology...
    System.setProperty(ConfigConstants.ENABLED_PROTOCOLS + DOT + ConfigConstants.BLAST_ENABLED, "true");
  }

  protected abstract Logger getLogger();

  protected abstract Topology getTopology();

  /**
   * Helper method to testing ping functionality. In a real system this would not function this way because this
   * methodology simply uses a Connection at a particular Connector (identified by {@code senderNodeAddress}) but this
   * connection doesn't actually flow through the packet-switch, so no balances are affected. To do this properly, an
   * actual sender should be used to ping a connector via the peer'd Connector.
   *
   * @param senderAccountId    The {@link AccountId} of the account to source the ping from.
   * @param senderNodeAddress  The {@link InterledgerAddress} for the node to use for initiating the ping (using {@code
   *                           senderAccountId}).
   * @param destinationAddress The {@link InterledgerAddress} to ping.
   * @param numUnits           A {@link BigInteger} representing the number of units to ping with.
   */
  protected InterledgerResponsePacket testPing(
    final AccountId senderAccountId,
    final InterledgerAddress senderNodeAddress,
    final InterledgerAddress destinationAddress,
    final UnsignedLong numUnits
  ) throws InterruptedException {
    return testPing(senderAccountId, senderNodeAddress, destinationAddress, numUnits, false);
  }

  /**
   * Helper method to testing ping functionality. In a real system this would not function this way because this
   * methodology simply uses a Connection at a particular Connector (identified by {@code senderNodeAddress}) but this
   * connection doesn't actually flow through the packet-switch, so no balances are affected. To do this properly, an
   * actual sender should be used to ping a connector via the peer'd Connector.
   *
   * @param senderAccountId    The {@link AccountId} of the account to source the ping from.
   * @param senderNodeAddress  The {@link InterledgerAddress} for the node to use for initiating the ping (using {@code
   *                           senderAccountId}).
   * @param destinationAddress The {@link InterledgerAddress} to ping.
   * @param numUnits           A {@link BigInteger} representing the number of units to ping with.
   */
  protected InterledgerResponsePacket testPing(
    final AccountId senderAccountId,
    final InterledgerAddress senderNodeAddress,
    final InterledgerAddress destinationAddress,
    final UnsignedLong numUnits,
    final boolean allowReject
  )
    throws InterruptedException {

    Objects.requireNonNull(senderNodeAddress);
    Objects.requireNonNull(senderAccountId);
    Objects.requireNonNull(destinationAddress);

    final CountDownLatch latch = new CountDownLatch(1);
    final long start = System.currentTimeMillis();

    final IlpOverHttpLink blastLink = getIlpOverHttpLinkFromGraph(senderNodeAddress, senderAccountId);
    final PingInitiator pingInitiator = new DefaultPingInitiator(blastLink, () -> Instant.now().plusSeconds(30));
    AtomicReference<InterledgerResponsePacket> response = new AtomicReference<>();
    pingInitiator.ping(destinationAddress, numUnits).handle(
      fulfillPacket -> {
        assertThat(fulfillPacket.getFulfillment(), is(PingLoopbackLink.PING_PROTOCOL_FULFILLMENT));
        assertThat(
          fulfillPacket.getFulfillment().validateCondition(PingLoopbackLink.PING_PROTOCOL_CONDITION),
          is(true)
        );
        latch.countDown();
        response.set(fulfillPacket);
      }, interledgerRejectPacket -> {
        if (!allowReject) {
          fail(String.format("Ping request rejected, but should have fulfilled: %s", interledgerRejectPacket));
        }
        latch.countDown();
        response.set(interledgerRejectPacket);
      }
    );

    latch.await(5, TimeUnit.SECONDS);
    final long end = System.currentTimeMillis();
    getLogger().info("Ping took {}ms", end - start);
    return response.get();
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
    Node node = getNode(interledgerAddress);
    return ((ConnectorServer) node.getContentObject()).getContext()
        .getBean(ILPv4Connector.class);
  }

  protected Node getNode(InterledgerAddress interledgerAddress) {
    Objects.requireNonNull(interledgerAddress);
    return getTopology().getNode(interledgerAddress.getValue());
  }

  protected AccountSettings getAccountSettings(InterledgerAddress interledgerAddress, AccountId accountId) {
    return ((ConnectorServerNode) getNode(interledgerAddress)).getAccountSettings(accountId);
  }

  /**
   * Helper method to obtain an instance of {@link RedisTemplate} that underpings the {@link BalanceTracker} for a given
   * node, if Redis is enabled.
   *
   * @param interledgerAddress The Router ILP address of the node to obtain the tracker from.
   *
   * @return An optionally-present instance of {@link RedisBalanceTracker}.
   */
  protected Optional<RedisTemplate<String, Object>> getRedisTemplate(final InterledgerAddress interledgerAddress) {
    Objects.requireNonNull(interledgerAddress);

    Object redisTemplateObject =
      ((ConnectorServer) getTopology().getNode(interledgerAddress.getValue()).getContentObject())
        .getContext()
        .getBean(BALANCE_TRACKING_JACKSON_REDIS_TEMPLATE_BEAN_NAME);

    if (redisTemplateObject != null) {
      return Optional.ofNullable((RedisTemplate) redisTemplateObject);
    } else {
      return Optional.empty();
    }
  }

  /**
   * Helper method to obtain an instance of {@link Link} from the topology, based upon its Interledger Address.
   *
   * @param nodeAddress The {@link InterledgerAddress} of the node in the graph to obtain a Link from.
   * @param accountId   The unique account identifier for the Link to return.*
   *
   * @return A {@link IlpOverHttpLink} in the Topology that corresponds to the supplied inputs.
   */
  protected IlpOverHttpLink getIlpOverHttpLinkFromGraph(final InterledgerAddress nodeAddress,
    final AccountId accountId) {
    Objects.requireNonNull(nodeAddress);
    Objects.requireNonNull(accountId);

    final Link link = getILPv4NodeFromGraph(nodeAddress).getLinkManager()
      .getOrCreateLink(accountId);

    if (IlpOverHttpLink.LINK_TYPE.equals(link.getLinkSettings().getLinkType())) {
      // Most of the time, this link is a CircuitBreaking link, in which case the IlpOverHttpLink is the Delegate.
      if (CircuitBreakingLink.class.isAssignableFrom(link.getClass())) {
        return ((CircuitBreakingLink) link).getLinkDelegateTyped();
      } else {
        return (IlpOverHttpLink) link;
      }
    } else {
      throw new LinkException(
        "Link was not of Type(BLAST), but was instead: " + link.getLinkSettings().getLinkType().value(),
        link.getLinkId());
    }
  }

  protected void assertAccountBalance(
    final ILPv4Connector connector,
    final AccountId accountId,
    final BigInteger expectedAmount
  ) {
    assertThat(
      String.format("Incorrect balance for `%s` @ `%s`!", accountId, connector.toString()),
      connector.getBalanceTracker().balance(accountId).netBalance(), is(expectedAmount)
    );
  }

  /**
   * Helper method to reset any balance tracking accounts so that each test run can start off with a clean slate. Note
   * that we _could_ expose a method on {@link BalanceTracker} called "resetBalance", but this is not currently a
   * requirement for any business logic, so it was decided to not enable this behavior in order to avoid a potential
   * foot-gun in production code (i.e., resetting balances is only something needed by integration tests at present).
   */
  protected void resetBalanceTracking() {
    final ILPv4Connector aliceConnector = this.getILPv4NodeFromGraph(getAliceConnectorAddress());
    final ILPv4Connector bobConnector = this.getILPv4NodeFromGraph(getBobConnectorAddress());

    // ITs should not be running with the InMemoryBalanceTracker, but sometimes they do such as when running from an
    // IDE where maven-exec-plugin doesn't startup Redis.
    if (InMemoryBalanceTracker.class.isAssignableFrom(aliceConnector.getBalanceTracker().getClass())) {
      ((InMemoryBalanceTracker) aliceConnector.getBalanceTracker()).resetAllBalances();
      ((InMemoryBalanceTracker) bobConnector.getBalanceTracker()).resetAllBalances();
    } else {
      // Reset the balances for any peering accounts in the Topology.
      RedisKeyCommands redisCommands = this.getRedisTemplate(getAliceConnectorAddress())
        .map(RedisTemplate::getConnectionFactory)
        .map(RedisConnectionFactory::getConnection)
        .map(RedisConnection::keyCommands)
        .orElseThrow(() -> new RedisConnectionFailureException("Unable to get redisHashCommands "));

      redisCommands.del(
        ("accounts:" + PING_ACCOUNT_ID).getBytes(),
        ("accounts:" + ALICE).getBytes(),
        ("accounts:" + BOB).getBytes(),
        ("accounts:" + PAUL).getBytes(),
        ("accounts:" + PETER).getBytes()
      );
    }
  }

  /**
   * Abstract method to obtain a reference to the ILP Address for the BOB Connector. This is necessary because
   * sub-classes of this abstract class may not all use the same ILP address for each Connector.
   */
  protected abstract InterledgerAddress getAliceConnectorAddress();

  /**
   * Abstract method to obtain a reference to the ILP Address for the BOB Connector. This is necessary because
   * sub-classes of this abstract class may not all use the same ILP address for each Connector.
   */
  protected abstract InterledgerAddress getBobConnectorAddress();
}
