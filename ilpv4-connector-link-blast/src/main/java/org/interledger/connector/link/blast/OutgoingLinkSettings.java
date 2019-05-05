package org.interledger.connector.link.blast;

import okhttp3.HttpUrl;
import org.immutables.value.Value;
import org.immutables.value.Value.Modifiable;
import org.interledger.connector.link.blast.tokenSettings.SharedSecretTokenSettings;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.interledger.connector.link.blast.BlastLinkSettings.AUTH_TYPE;
import static org.interledger.connector.link.blast.BlastLinkSettings.BLAST;
import static org.interledger.connector.link.blast.BlastLinkSettings.DOT;
import static org.interledger.connector.link.blast.BlastLinkSettings.OUTGOING;
import static org.interledger.connector.link.blast.BlastLinkSettings.SHARED_SECRET;
import static org.interledger.connector.link.blast.BlastLinkSettings.TOKEN_AUDIENCE;
import static org.interledger.connector.link.blast.BlastLinkSettings.TOKEN_EXPIRY;
import static org.interledger.connector.link.blast.BlastLinkSettings.TOKEN_ISSUER;
import static org.interledger.connector.link.blast.BlastLinkSettings.TOKEN_SUBJECT;
import static org.interledger.connector.link.blast.BlastLinkSettings.URL;

public interface OutgoingLinkSettings extends SharedSecretTokenSettings {

  String BLAST_OUTGOING_AUTH_TYPE = BLAST + DOT + OUTGOING + DOT + AUTH_TYPE;

  String BLAST_OUTGOING_TOKEN_ISSUER = BLAST + DOT + OUTGOING + DOT + TOKEN_ISSUER;
  String BLAST_OUTGOING_TOKEN_AUDIENCE = BLAST + DOT + OUTGOING + DOT + TOKEN_AUDIENCE;
  String BLAST_OUTGOING_TOKEN_SUBJECT = BLAST + DOT + OUTGOING + DOT + TOKEN_SUBJECT;
  String BLAST_OUTGOING_TOKEN_EXPIRY = BLAST + DOT + OUTGOING + DOT + TOKEN_EXPIRY;

  String BLAST_OUTGOING_SHARED_SECRET = BLAST + DOT + OUTGOING + DOT + SHARED_SECRET;
  String BLAST_OUTGOING_URL = BLAST + DOT + OUTGOING + DOT + URL;

  static ImmutableOutgoingLinkSettings.Builder builder() {
    return ImmutableOutgoingLinkSettings.builder();
  }

  /**
   * Constructs a new builder with the correct custom settings, as found in {@code customSettings}.
   *
   * @param customSettings
   *
   * @return
   */
  static ImmutableOutgoingLinkSettings.Builder fromCustomSettings(Map<String, Object> customSettings) {
    Objects.requireNonNull(customSettings);

    return applyCustomSettings(ImmutableOutgoingLinkSettings.builder(), customSettings);
  }

  /**
   * Populate a supplied builder with the correct custom settings, as found in {@code customSettings}.
   *
   * @param builder
   * @param customSettings
   *
   * @return
   */
  static ImmutableOutgoingLinkSettings.Builder applyCustomSettings(
    final ImmutableOutgoingLinkSettings.Builder builder, Map<String, Object> customSettings
  ) {
    Objects.requireNonNull(builder);
    Objects.requireNonNull(customSettings);

    // When loaded from a properties file, the properties are hierarchical in a Map. However, in Java, they are not, so
    // consider both options. Generally only one will be present, but if for some reason both are present, the String
    // values will win.
    Optional.ofNullable(customSettings.get(BLAST))
      .map(val -> (Map<String, Object>) val)
      .ifPresent(blastSettings -> {

        Optional.ofNullable(blastSettings.get(OUTGOING))
          .map(val -> (Map<String, Object>) val)
          .ifPresent(outgoingSettings -> {

            Optional.ofNullable(outgoingSettings.get(TOKEN_SUBJECT))
              .map(Object::toString)
              .ifPresent(builder::tokenSubject);

            Optional.ofNullable(outgoingSettings.get(TOKEN_ISSUER))
              .map(Object::toString)
              .map(HttpUrl::parse)
              .ifPresent(builder::tokenIssuer);

            Optional.ofNullable(outgoingSettings.get(TOKEN_AUDIENCE))
              .map(Object::toString)
              .ifPresent(builder::tokenAudience);

            Optional.ofNullable(outgoingSettings.get(AUTH_TYPE))
              .map(Object::toString)
              .map(String::toUpperCase)
              .map(BlastLinkSettings.AuthType::valueOf)
              .ifPresent(builder::authType);

            Optional.ofNullable(outgoingSettings.get(SHARED_SECRET))
              .map(Object::toString)
              .ifPresent(builder::encryptedTokenSharedSecret);

            Optional.ofNullable(outgoingSettings.get(TOKEN_EXPIRY))
              .map(Object::toString)
              .map(Duration::parse)
              .ifPresent(builder::tokenExpiry);

            Optional.ofNullable(outgoingSettings.get(URL))
              .map(Object::toString)
              .map(HttpUrl::parse)
              .ifPresent(builder::url);

          });
      });

    Optional.ofNullable(customSettings.get(BLAST_OUTGOING_TOKEN_ISSUER))
      .map(Object::toString)
      .map(HttpUrl::parse)
      .ifPresent(builder::tokenIssuer);

    Optional.ofNullable(customSettings.get(BLAST_OUTGOING_TOKEN_AUDIENCE))
      .map(Object::toString)
      .ifPresent(builder::tokenAudience);

    Optional.ofNullable(customSettings.get(BLAST_OUTGOING_AUTH_TYPE))
      .map(Object::toString)
      .map(String::toUpperCase)
      .map(BlastLinkSettings.AuthType::valueOf)
      .ifPresent(builder::authType);

    Optional.ofNullable(customSettings.get(BLAST_OUTGOING_TOKEN_SUBJECT))
      .map(Object::toString)
      .ifPresent(builder::tokenSubject);

    Optional.ofNullable(customSettings.get(BLAST_OUTGOING_SHARED_SECRET))
      .map(Object::toString)
      .ifPresent(builder::encryptedTokenSharedSecret);

    Optional.ofNullable(customSettings.get(BLAST_OUTGOING_TOKEN_EXPIRY))
      .map(Object::toString)
      .map(Duration::parse)
      .ifPresent(builder::tokenExpiry);

    Optional.ofNullable(customSettings.get(BLAST_OUTGOING_URL))
      .map(Object::toString)
      .map(HttpUrl::parse)
      .ifPresent(builder::url);

    return builder;
  }

  /**
   * If present, determines how often to sign a new token for auth. Optional to support the shared-secret use-case.
   *
   * @return
   */
  Optional<Duration> tokenExpiry();

  /**
   * endpoint to POST packets to. If url contains a percent and the link is in `multi` mode, then the segment after this
   * link's own address will be filled where the `%` is  when routing packets.
   *
   * @return
   */
  HttpUrl url();

  @Value.Immutable
  @Modifiable
  abstract class AbstractOutgoingLinkSettings implements OutgoingLinkSettings {

    @Override
    @Value.Redacted
    public abstract String encryptedTokenSharedSecret();

  }
}
