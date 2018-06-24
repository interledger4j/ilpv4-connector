package com.sappenin.ilpv4.plugins;

import com.sappenin.ilpv4.model.InterledgerAddress;
import com.sappenin.ilpv4.model.Plugin;

import java.util.Optional;

public interface PluginManager {

  /**
   * Associate the supplied {@code plugin} to the supplied {@code accountAddress}.
   *
   * @param accountAddress
   * @param plugin
   */
  void setPlugin(InterledgerAddress accountAddress, Plugin plugin);

  /**
   * <p>Retrieve a plugin for the supplied {@code accountAddress}.</p>
   *
   * <p>Note that this method returns a one or zeor plugins using an exact-match algorithm because a particular
   * account can have only one plugin at a time.</p>
   *
   * @param accountAddress
   *
   * @return
   */
  Optional<Plugin> getPlugin(InterledgerAddress accountAddress);

}
