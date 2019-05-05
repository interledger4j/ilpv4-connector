package org.interledger.connector.link.blast;

import org.immutables.value.Value;
import org.immutables.value.Value.Modifiable;
import org.interledger.connector.link.LinkSettings;

import java.util.Map;
import java.util.Objects;

public interface BlastLinkSettings extends LinkSettings {

  String DOT = ".";
  String BLAST = "blast";

  String OUTGOING = "outgoing";
  String INCOMING = "incoming";

  String AUTH_TYPE = "auth_type";

  String TOKEN_ISSUER = "token_issuer";
  String TOKEN_AUDIENCE = "token_audience";
  String TOKEN_SUBJECT = "token_subject";
  String TOKEN_EXPIRY = "token_expiry";

  // Used to grab the auth credential from custom settings...
  String SHARED_SECRET = "shared_secret";
  String URL = "url";

  static ImmutableBlastLinkSettings.Builder builder() {
    return ImmutableBlastLinkSettings.builder();
  }

  /**
   * Constructs a new builder with the correct custom settings, as found in {@code customSettings}.
   *
   * @param customSettings
   *
   * @return
   */
  static ImmutableBlastLinkSettings.Builder fromCustomSettings(final Map<String, Object> customSettings) {
    Objects.requireNonNull(customSettings);
    return applyCustomSettings(BlastLinkSettings.builder(), customSettings)
      .linkType(BlastLink.LINK_TYPE);
  }

  /**
   * Populate a supplied builder with the correct custom settings, as found in {@code customSettings}.
   *
   * @param builder
   * @param customSettings
   *
   * @return
   */
  static ImmutableBlastLinkSettings.Builder applyCustomSettings(
    final ImmutableBlastLinkSettings.Builder builder, Map<String, Object> customSettings
  ) {
    Objects.requireNonNull(builder);
    Objects.requireNonNull(customSettings);

    final ImmutableIncomingLinkSettings.Builder incomingLinkSettingsBuilder =
      IncomingLinkSettings.fromCustomSettings(customSettings);
    final ImmutableOutgoingLinkSettings.Builder outgoingLinkSettingsBuilder =
      OutgoingLinkSettings.fromCustomSettings(customSettings);

    builder.incomingBlastLinkSettings(incomingLinkSettingsBuilder.build());
    builder.outgoingBlastLinkSettings(outgoingLinkSettingsBuilder.build());

    builder.customSettings(customSettings);

    return builder;
  }

  /**
   * Link settings for the incoming BLAST link.
   *
   * @return A {@link IncomingLinkSettings}.
   */
  IncomingLinkSettings incomingBlastLinkSettings();

  /**
   * Link settings for the outgoing BLAST link.
   *
   * @return A {@link OutgoingLinkSettings}.
   */
  OutgoingLinkSettings outgoingBlastLinkSettings();

  enum AuthType {
    /**
     * The incoming and outgoing secrets are used as Bearer tokens in an HTTP Authorization header.
     */
    SIMPLE,
    /**
     * Use shared-secret symmetric keys to create and verify JWT_HS_256 tokens.
     */
    JWT_HS_256,
    /**
     * Use RSA asymmetric keys to create aand verify JWT_RS_256 tokens.
     */
    //JWT_RS_256
  }

  @Value.Immutable
  @Modifiable
  abstract class AbstractBlastLinkSettings implements BlastLinkSettings {

  }
}
