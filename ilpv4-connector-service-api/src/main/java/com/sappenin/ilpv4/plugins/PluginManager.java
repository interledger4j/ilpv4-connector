package com.sappenin.ilpv4.plugins;

import org.interledger.core.InterledgerAddress;
import org.interledger.plugin.lpiv2.Plugin;

import java.util.Optional;

public interface PluginManager {

  /**
   * Associate the supplied {@code lpi2} to the supplied {@code accountAddress}.
   *
   * @param accountAddress
   * @param plugin
   */
  void setPlugin(InterledgerAddress accountAddress, Plugin plugin);

  /**
   * <p>Retrieve a {@link Plugin} for the supplied {@code accountAddress}.</p>
   *
   * <p>Note that this method returns one or zero plugins using an exact-match algorithm on the address because a
   * particular account can have only one plugin at a time.</p>
   *
   * @param peerAccountAddress The {@link InterledgerAddress} of the remote peer.
   *
   * @return An optinoally-present {@link Plugin}.
   */
  Optional<Plugin> getPlugin(InterledgerAddress peerAccountAddress);

  /**
   * <p>Retrieve a {@link Plugin} for the supplied {@code accountAddress}, or construct a new instance if one does
   * not exist already.</p>
   *
   * @param peerAccountAddress
   *
   * @return
   *
   * @deprecated This method should not be used, instead favor creating a plugin at Account construction time.
   */
  //@Deprecated
  //Plugin getOrCreatePlugin(InterledgerAddress peerAccountAddress);

}
