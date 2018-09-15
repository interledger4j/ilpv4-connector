package com.sappenin.ilpv4.plugins;

import com.sappenin.ilpv4.model.settings.ConnectorSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.plugin.lpiv2.AbstractPlugin;
import org.interledger.plugin.lpiv2.Plugin;
import org.interledger.plugin.lpiv2.PluginSettings;

import java.util.Objects;

// TODO make this implement all BTP functionality, and make setttle/sendMoney a no-op so that this can be used to
// represent a data channel with no money involved.


// TODO: Is this used for BTP?

/**
 * An implementation of {@link Plugin} that simulates a relationship with a parent node where this connector is the
 * child.
 */
public abstract class AbstractBtpPlugin {// extends AbstractPlugin<PluginSettings> implements Plugin {

//  // The ILP Address of the Connector operating this lpi2.
//  private final ConnectorSettings connectorSettings;
//
//  /**
//   * Required-args Constructor.
//   *
//   * @param connectorSettings
//   * @param accountAddress    The Interledger Address for this lpi2.
//   */
//  public AbstractBtpPlugin(final ConnectorSettings connectorSettings, final InterledgerAddress accountAddress) {
//    this.connectorSettings = Objects.requireNonNull(connectorSettings);
//  }
}