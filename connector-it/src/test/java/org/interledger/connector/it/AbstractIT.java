package org.interledger.connector.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.connector.accounts.sub.LocalDestinationAddressUtils.PING_ACCOUNT_ID;
import static org.interledger.connector.config.BalanceTrackerConfig.BALANCE_TRACKING_JACKSON_REDIS_TEMPLATE_BEAN_NAME;
import static org.interledger.connector.core.ConfigConstants.ADMIN_PASSWORD;
import static org.interledger.connector.core.ConfigConstants.DOT;
import static org.interledger.connector.core.ConfigConstants.INTERLEDGER__CONNECTOR__GLOBAL_ROUTING_SETTINGS__ROUTING_SECRET;
import static org.interledger.connector.it.topologies.AbstractTopology.ALICE;
import static org.interledger.connector.it.topologies.AbstractTopology.BOB;
import static org.interledger.connector.it.topologies.AbstractTopology.PAUL;
import static org.interledger.connector.it.topologies.AbstractTopology.PETER;
import static org.interledger.crypto.CryptoConfigConstants.INTERLEDGER_CONNECTOR_KEYSTORE_JKS_ENABLED;
import static org.interledger.crypto.CryptoConfigConstants.INTERLEDGER_CONNECTOR_KEYSTORE_JKS_FILENAME;
import static org.interledger.crypto.CryptoConfigConstants.INTERLEDGER_CONNECTOR_KEYSTORE_JKS_PASSWORD;
import static org.interledger.crypto.CryptoConfigConstants.INTERLEDGER_CONNECTOR_KEYSTORE_JKS_SECRET0_ALIAS;
import static org.interledger.crypto.CryptoConfigConstants.INTERLEDGER_CONNECTOR_KEYSTORE_JKS_SECRET0_PASSWORD;

import org.interledger.connector.ILPv4Connector;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.balances.BalanceTracker;
import org.interledger.connector.balances.InMemoryBalanceTracker;
import org.interledger.connector.balances.RedisBalanceTracker;
import org.interledger.connector.core.ConfigConstants;
import org.interledger.connector.it.topology.AbstractBaseTopology;
import org.interledger.connector.it.topology.Node;
import org.interledger.connector.it.topology.nodes.ConnectorServerNode;
import org.interledger.connector.server.ConnectorServer;
import org.interledger.connector.server.spring.settings.javamoney.SpringServiceProvider;
import org.interledger.core.InterledgerAddress;
import org.interledger.link.http.IlpOverHttpLinkSettings;

import feign.RequestInterceptor;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisKeyCommands;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;

import javax.money.spi.Bootstrap;

/**
 * Abstract parent class for all Integration tests.
 */
public abstract class AbstractIT {

  @BeforeClass
  public static void setupClass() {
    // For whatever reason, the JavaMoney Bootstrap mechanism doesn't pickup the service provider directive in
    // `src/main/resources/META-INF/services/javax.money.spi.ServiceProvider` in the `connector-server` module.
    // Thus, we need to programmatically wire-in JavaMoney into Spring using this manual call (before any JavaMoney
    // code is engaged).
    Bootstrap.init(new SpringServiceProvider());

    System.setProperty(ADMIN_PASSWORD, "password");

    // Configure JKS Properly for test purposes
    // For dev/test purposes this is fine, but not for real use-cases. Use KMS instead.
    System.setProperty(INTERLEDGER_CONNECTOR_KEYSTORE_JKS_ENABLED, "true");
    System.setProperty(INTERLEDGER_CONNECTOR_KEYSTORE_JKS_FILENAME, "crypto/crypto.p12");
    System.setProperty(INTERLEDGER_CONNECTOR_KEYSTORE_JKS_PASSWORD, "password");
    // For dev/test purposes this is fine, but not for real use-cases. Encrypt this value instead.
    System.setProperty(INTERLEDGER_CONNECTOR_KEYSTORE_JKS_SECRET0_ALIAS, "secret0");
    System.setProperty(INTERLEDGER_CONNECTOR_KEYSTORE_JKS_SECRET0_PASSWORD, "password");
    //A simulated routing secret, which is a seed used for generating routing table auth values. Represents the
    // plaintext value of `shh`, encrypted.
    System.setProperty(INTERLEDGER__CONNECTOR__GLOBAL_ROUTING_SETTINGS__ROUTING_SECRET,
      "enc:JKS:crypto.p12:secret0:1:aes_gcm:AAAADKZPmASojt1iayb2bPy4D-Toq7TGLTN95HzCQAeJtz0=");

    // Required to get the conditional-config to work for this topology...
    System.setProperty(ConfigConstants.ENABLED_PROTOCOLS + DOT + ConfigConstants.ILP_OVER_HTTP_ENABLED, "true");
  }

  protected abstract Logger getLogger();

  protected abstract AbstractBaseTopology getTopology();

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

  protected IlpOverHttpLinkSettings getLinkSettings(InterledgerAddress interledgerAddress, AccountId accountId) {
    return IlpOverHttpLinkSettings.fromCustomSettings(
      getAccountSettings(interledgerAddress, accountId).customSettings())
      .build();
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

  protected void assertAccountBalance(
    final ILPv4Connector connector,
    final AccountId accountId,
    final BigInteger expectedAmount
  ) {
    assertThat(connector.getBalanceTracker().balance(accountId).netBalance())
      .as(String.format("Incorrect balance for `%s` @ `%s`!", accountId, connector.toString()))
      .isEqualTo(expectedAmount);
  }

  /**
   * Helper method to reset any balance tracking accounts so that each test run can start off with a clean slate. Note
   * that we _could_ expose a method on {@link BalanceTracker} called "resetBalance", but this is not currently a
   * requirement for any business logic, so it was decided to not enable this behavior in order to avoid a potential
   * foot-gun in production code (i.e., resetting balances is only something needed by integration tests at present).
   */
  protected void resetBalanceTracking() {
    final ILPv4Connector aliceConnector = this.getILPv4NodeFromGraph(getAliceConnectorAddress());

    // ITs should not be running with the InMemoryBalanceTracker, but sometimes they do such as when running from an
    // IDE where maven-exec-plugin doesn't startup Redis.
    if (InMemoryBalanceTracker.class.isAssignableFrom(aliceConnector.getBalanceTracker().getClass())) {
      ((InMemoryBalanceTracker) aliceConnector.getBalanceTracker()).resetAllBalances();
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
   * Constructs a {@link RequestInterceptor} preconfigured to be able to talk to the connector.
   *
   * @return a request interceptor that allows login to the connector
   */
  protected RequestInterceptor basicAuthRequestInterceptor() {
    return template -> template.header("Authorization", "Basic YWRtaW46cGFzc3dvcmQ=");
  }
}
