package org.interledger.ilpv4.connector.it.topology;

import org.interledger.plugin.lpiv2.btp2.spring.AbstractBtpPlugin;

/**
 * A {@link PluginNode} that contains a data object of type {@link P} that extends {@link AbstractBtpPlugin}.
 *
 * @param <P>
 */
public interface BtpClientPluginNode<P extends AbstractBtpPlugin<?>> extends PluginNode<P> {
}
