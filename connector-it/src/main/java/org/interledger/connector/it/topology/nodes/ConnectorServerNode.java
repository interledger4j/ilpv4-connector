package org.interledger.connector.it.topology.nodes;

import org.interledger.connector.ILPv4Connector;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.it.topology.AbstractServerNode;
import org.interledger.connector.persistence.repositories.AccountSettingsRepository;
import org.interledger.connector.server.ConnectorServer;

import java.util.Objects;

/**
 * A Node that simulates an ILPv4 Connector.
 */
public class ConnectorServerNode extends AbstractServerNode<ConnectorServer> {

  private final String id;

  /**
   * Required-args Constructor.
   */
  public ConnectorServerNode(final String id, final ConnectorServer server) {
    super(server, "ws", "localhost");
    this.id = id;
  }



  @Override
  public String toString() {
    return getILPv4Connector().getConnectorSettings().operatorAddress().getValue();
  }

  /**
   * This node contains a Spring Server, so to return its {@link ILPv4Connector} we need to inspect the
   * application-context.
   */
  public ILPv4Connector getILPv4Connector() {
    return this.getServer().getContext().getBean(ILPv4Connector.class);
  }

  public AccountSettings getAccountSettings(AccountId accountId) {
    Objects.requireNonNull(accountId);
    return this.getServer().getContext().getBean(AccountSettingsRepository.class)
        .findByAccountIdWithConversion(accountId)
        .orElseThrow(() -> new IllegalArgumentException("No account exists for id " + accountId.value()));
  }

  @Override
  public String getId() {
    return this.id;
  }

  @Override
  public void stop() {
    super.stop();
  }
}
