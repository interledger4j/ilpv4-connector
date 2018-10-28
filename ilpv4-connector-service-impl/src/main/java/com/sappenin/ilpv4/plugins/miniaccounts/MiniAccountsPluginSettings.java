package com.sappenin.ilpv4.plugins.miniaccounts;

import org.immutables.value.Value;
import org.immutables.value.Value.Immutable;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.plugin.lpiv2.Plugin;
import org.interledger.plugin.lpiv2.PluginSettings;

import java.util.Optional;

/**
 * Configuration information relating to a {@link Plugin}.
 */
public interface MiniAccountsPluginSettings extends PluginSettings {

  int currencyScale();

  @Value.Default
  default String allowedOrigins() {
    return "*";
  }

  /**
   * An optionally-defined prefix. If left empty in this configuration, ILDCP will be used instead to determine this
   * prefix.
   */
  Optional<InterledgerAddressPrefix> prefix();

  @Immutable
  abstract class AbstractMinAccountsPluginSettings implements MiniAccountsPluginSettings {

  }

}
