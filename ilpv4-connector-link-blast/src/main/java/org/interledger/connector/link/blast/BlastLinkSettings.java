package org.interledger.connector.link.blast;

import okhttp3.HttpUrl;
import org.immutables.value.Value;
import org.immutables.value.Value.Modifiable;
import org.interledger.connector.link.LinkSettings;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.interledger.connector.link.blast.IncomingLinkSettings.BLAST_INCOMING_SHARED_SECRET;
import static org.interledger.connector.link.blast.IncomingLinkSettings.BLAST_INCOMING_AUTH_TYPE;
import static org.interledger.connector.link.blast.IncomingLinkSettings.BLAST_INCOMING_TOKEN_AUDIENCE;
import static org.interledger.connector.link.blast.IncomingLinkSettings.BLAST_INCOMING_TOKEN_ISSUER;
import static org.interledger.connector.link.blast.IncomingLinkSettings.BLAST_INCOMING_TOKEN_SUBJECT;
import static org.interledger.connector.link.blast.OutgoingLinkSettings.BLAST_OUTGOING_AUTH_TYPE;
import static org.interledger.connector.link.blast.OutgoingLinkSettings.BLAST_OUTGOING_SHARED_SECRET;
import static org.interledger.connector.link.blast.OutgoingLinkSettings.BLAST_OUTGOING_TOKEN_AUDIENCE;
import static org.interledger.connector.link.blast.OutgoingLinkSettings.BLAST_OUTGOING_TOKEN_EXPIRY;
import static org.interledger.connector.link.blast.OutgoingLinkSettings.BLAST_OUTGOING_TOKEN_ISSUER;
import static org.interledger.connector.link.blast.OutgoingLinkSettings.BLAST_OUTGOING_URL;

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
    return applyCustomSettings(BlastLinkSettings.builder(), customSettings);
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

    final ImmutableIncomingLinkSettings.Builder incomingLinkSettingsBuilder = IncomingLinkSettings.builder();
    final ImmutableOutgoingLinkSettings.Builder outgoingLinkSettingsBuilder = OutgoingLinkSettings.builder();

    // When loaded from a properties file, the properties are hierarchical in a Map. However, in Java, they are not, so
    // consider both options. Generally only one will be present, but if for some reason both are present, the String
    // values will win.
    Optional.ofNullable(customSettings.get(BLAST))
      .map(val -> (Map<String, Object>) val)
      .ifPresent(blastSettings -> {

        Optional.ofNullable(blastSettings.get(INCOMING))
          .map(val -> (Map<String, Object>) val)
          .ifPresent($ -> IncomingLinkSettings.applyCustomSettings(incomingLinkSettingsBuilder, customSettings));

        Optional.ofNullable(blastSettings.get(OUTGOING))
          .map(val -> (Map<String, Object>) val)
          .ifPresent($ -> OutgoingLinkSettings.applyCustomSettings(outgoingLinkSettingsBuilder, customSettings));

      });

    // Flattened Incoming settings...
    Optional.ofNullable(customSettings.get(BLAST_INCOMING_AUTH_TYPE))
      .map(Object::toString)
      .map(String::toUpperCase)
      .map(BlastLinkSettings.AuthType::valueOf)
      .ifPresent(incomingLinkSettingsBuilder::authType);

    Optional.ofNullable(customSettings.get(BLAST_INCOMING_TOKEN_ISSUER))
      .map(Object::toString)
      .map(HttpUrl::parse)
      .ifPresent(incomingLinkSettingsBuilder::tokenIssuer);

    Optional.ofNullable(customSettings.get(BLAST_INCOMING_TOKEN_AUDIENCE))
      .map(Object::toString)
      .ifPresent(incomingLinkSettingsBuilder::tokenAudience);

    Optional.ofNullable(customSettings.get(BLAST_INCOMING_TOKEN_SUBJECT))
      .map(Object::toString)
      .ifPresent(incomingLinkSettingsBuilder::tokenSubject);

    Optional.ofNullable(customSettings.get(BLAST_INCOMING_SHARED_SECRET))
      .map(Object::toString)
      .ifPresent(incomingLinkSettingsBuilder::encryptedTokenSharedSecret);

    // Flattened Outgoing Settings
    Optional.ofNullable(customSettings.get(BLAST_OUTGOING_TOKEN_ISSUER))
      .map(Object::toString)
      .map(HttpUrl::parse)
      .ifPresent(outgoingLinkSettingsBuilder::tokenIssuer);

    Optional.ofNullable(customSettings.get(BLAST_OUTGOING_TOKEN_AUDIENCE))
      .map(Object::toString)
      .ifPresent(outgoingLinkSettingsBuilder::tokenAudience);

    Optional.ofNullable(customSettings.get(BLAST_OUTGOING_AUTH_TYPE))
      .map(Object::toString)
      .map(String::toUpperCase)
      .map(BlastLinkSettings.AuthType::valueOf)
      .ifPresent(outgoingLinkSettingsBuilder::authType);

    Optional.ofNullable(customSettings.get(BLAST_INCOMING_TOKEN_SUBJECT))
      .map(Object::toString)
      .ifPresent(outgoingLinkSettingsBuilder::tokenSubject);

    Optional.ofNullable(customSettings.get(BLAST_OUTGOING_SHARED_SECRET))
      .map(Object::toString)
      .ifPresent(outgoingLinkSettingsBuilder::encryptedTokenSharedSecret);

    Optional.ofNullable(customSettings.get(BLAST_OUTGOING_TOKEN_EXPIRY))
      .map(Object::toString)
      .map(Duration::parse)
      .ifPresent(outgoingLinkSettingsBuilder::tokenExpiry);

    Optional.ofNullable(customSettings.get(BLAST_OUTGOING_URL))
      .map(Object::toString)
      .map(HttpUrl::parse)
      .ifPresent(outgoingLinkSettingsBuilder::url);

    builder.customSettings(customSettings);

    builder.incomingBlastLinkSettings(incomingLinkSettingsBuilder.build());
    builder.outgoingBlastLinkSettings(outgoingLinkSettingsBuilder.build());

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
    JWT_RS_256
  }

  @Value.Immutable
  @Modifiable
  abstract class AbstractBlastLinkSettings implements BlastLinkSettings {

  }
}
