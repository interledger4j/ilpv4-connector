package com.sappenin.interledger.ilpv4.connector.settings;

import org.interledger.plugin.lpiv2.PluginSettings;

import java.util.Map;
import java.util.Objects;

/**
 * A factory for assembling instances of {@link PluginSettings} from a custom-settings {@link Map}.
 */
public class PluginSettingsFactory {

  /**
   * Helper class to assemble an instance of {@link T}
   *
   * @param customSettings
   * @param pluginClass
   * @param <T>
   *
   * @return
   */
  public static <T extends PluginSettings> T getPluginSettings(
    final Map<String, Object> customSettings, final Class<T> pluginClass
  ) {
    Objects.requireNonNull(customSettings);
    Objects.requireNonNull(pluginClass);

    throw new RuntimeException("FINISH ME!");

  }


}
