package com.sappenin.ilpv4.plugins.btp;

import com.google.common.base.Preconditions;
import org.immutables.value.Value;
import org.interledger.plugin.lpiv2.PluginSettings;

import java.util.Map;
import java.util.Objects;

@Value.Immutable
public interface BtpClientPluginSettings extends BtpPluginSettings {

  /**
   * The scheme for the remote BTP peer connection.
   */
  String KEY_REMOTE_PEER_SCHEME = "remotePeerScheme";

  /**
   * The hostname for the remote BTP peer.
   */
  String KEY_REMOTE_PEER_HOSTNAME = "remotePeerHostName";

  /**
   * The port for the remote BTP peer.
   */
  String KEY_REMOTE_PEER_PORT = "remotePeerPort";

  /**
   * Construct a new Builder.
   *
   * @return A new instance of {@link ImmutableBtpClientPluginSettings.Builder}.
   */
  static ImmutableBtpClientPluginSettings.Builder builder() {
    return ImmutableBtpClientPluginSettings.builder();
  }

  /**
   * Populate a new {@link ImmutableBtpClientPluginSettings.Builder} with the correct custom settings, as found in
   * {@code customSettings}.
   *
   * @param pluginSettings
   *
   * @return A new instance of {@link ImmutableBtpClientPluginSettings.Builder} with custom-settings properly
   * initialized.
   */
  static ImmutableBtpClientPluginSettings.Builder fromPluginSettingsWithCustomSettings(final PluginSettings pluginSettings) {
    Objects.requireNonNull(pluginSettings);

    return withCustomSettings(
      ImmutableBtpClientPluginSettings.builder().from(pluginSettings), pluginSettings.getCustomSettings()
    );
  }

  /**
   * Populate a supplied builder with the correct custom settings, as found in {@code customSettings}.
   *
   * @param builder
   * @param customSettings
   *
   * @return
   */
  static ImmutableBtpClientPluginSettings.Builder withCustomSettings(
    final ImmutableBtpClientPluginSettings.Builder builder, Map<String, Object> customSettings
  ) {
    Objects.requireNonNull(builder);

    return builder
      .secret(
        Objects.requireNonNull(customSettings.get(KEY_SECRET), "`secret` not found in customSettings!").toString()
      )
      .remotePeerScheme(
        Objects
          .requireNonNull(customSettings.get(KEY_REMOTE_PEER_SCHEME), "`remotePeerScheme` not found in customSettings!")
          .toString()
      )
      .remotePeerHostname(
        Objects.requireNonNull(customSettings.get(KEY_REMOTE_PEER_HOSTNAME),
          "`remotePeerHostname` not found in customSettings!").toString()
      )
      .remotePeerPort(Objects
        .requireNonNull(customSettings.get(KEY_REMOTE_PEER_PORT), "`remotePeerPort` not found in customSettings!")
        .toString());
  }

  /**
   * The scheme for the remote peer connection. Currently only "ws" and "wss" are supported.
   *
   * @return
   */
  @Value.Default
  default String getRemotePeerScheme() {
    return "wss";
  }

  /**
   * The hostname for the remote BTP peer.
   */
  @Value.Default
  default String getRemotePeerHostname() {
    return "localhost";
  }

  /**
   * The port for the remote BTP peer.
   */
  @Value.Default
  default String getRemotePeerPort() {
    return "6666";
  }

  @Value.Check
  default void check() {
    Preconditions.checkArgument("ws".equals(this.getRemotePeerScheme()) || "wss".equals(this.getRemotePeerScheme()),
      "Remote Peer scheme must be either `ws` or `wss`");
  }
}
