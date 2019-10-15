package org.interledger.connector.link.blast;

import static org.interledger.connector.link.blast.BlastLinkSettings.AUTH_TYPE;
import static org.interledger.connector.link.blast.BlastLinkSettings.BLAST;
import static org.interledger.connector.link.blast.BlastLinkSettings.DOT;
import static org.interledger.connector.link.blast.BlastLinkSettings.INCOMING;
import static org.interledger.connector.link.blast.BlastLinkSettings.SHARED_SECRET;
import static org.interledger.connector.link.blast.BlastLinkSettings.TOKEN_AUDIENCE;
import static org.interledger.connector.link.blast.BlastLinkSettings.TOKEN_ISSUER;

import org.interledger.connector.link.blast.tokenSettings.SharedSecretTokenSettings;

import okhttp3.HttpUrl;
import org.immutables.value.Value;
import org.immutables.value.Value.Modifiable;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * An extension of {@link SharedSecretTokenSettings} for incoming ILP-over-HTTP Links. Note that this interface
 * purposefully does not define a `sub` claim because the `accountId` for any given account should always be used
 * instead for verification purposes.
 */
public interface IncomingLinkSettings extends SharedSecretTokenSettings {

  String BLAST_INCOMING_AUTH_TYPE = BLAST + DOT + INCOMING + DOT + AUTH_TYPE;

  String BLAST_INCOMING_TOKEN_ISSUER = BLAST + DOT + INCOMING + DOT + TOKEN_ISSUER;
  String BLAST_INCOMING_TOKEN_AUDIENCE = BLAST + DOT + INCOMING + DOT + TOKEN_AUDIENCE;
  String BLAST_INCOMING_SHARED_SECRET = BLAST + DOT + INCOMING + DOT + SHARED_SECRET;

  static ImmutableIncomingLinkSettings.Builder builder() {
    return ImmutableIncomingLinkSettings.builder();
  }

  /**
   * Constructs a new builder with the correct custom settings, as found in {@code customSettings}.
   *
   * @param customSettings
   *
   * @return
   */
  static ImmutableIncomingLinkSettings.Builder fromCustomSettings(Map<String, Object> customSettings) {
    Objects.requireNonNull(customSettings);

    return applyCustomSettings(ImmutableIncomingLinkSettings.builder(), customSettings);
  }

  /**
   * Populate a supplied builder with the correct custom settings, as found in {@code customSettings}.
   *
   * @param builder
   * @param customSettings
   *
   * @return
   */
  static ImmutableIncomingLinkSettings.Builder applyCustomSettings(
    final ImmutableIncomingLinkSettings.Builder builder, Map<String, Object> customSettings
  ) {
    Objects.requireNonNull(builder);
    Objects.requireNonNull(customSettings);

    // When loaded from a properties file, the properties are hierarchical in a Map. However, in Java, they are not, so
    // consider both options. Generally only one will be present, but if for some reason both are present, the String
    // values will win.
    Optional.ofNullable(customSettings.get(BLAST))
      .map(val -> (Map<String, Object>) val)
      .ifPresent(blastSettings -> Optional.ofNullable(blastSettings.get(INCOMING))
        .map(val -> (Map<String, Object>) val)
        .ifPresent(incomingSettings -> {

          Optional.ofNullable(incomingSettings.get(AUTH_TYPE))
            .map(Object::toString)
            .map(String::toUpperCase)
            .map(BlastLinkSettings.AuthType::valueOf)
            .ifPresent(builder::authType);

          Optional.ofNullable(incomingSettings.get(SHARED_SECRET))
            .map(Object::toString)
            .ifPresent(builder::encryptedTokenSharedSecret);

          Optional.ofNullable(incomingSettings.get(TOKEN_ISSUER))
            .map(Object::toString)
            .map(HttpUrl::parse)
            .ifPresent(builder::tokenIssuer);

          Optional.ofNullable(incomingSettings.get(TOKEN_AUDIENCE))
            .map(Object::toString)
            .map(HttpUrl::parse)
            .ifPresent(builder::tokenAudience);
        }));

    Optional.ofNullable(customSettings.get(BLAST_INCOMING_AUTH_TYPE))
      .map(Object::toString)
      .map(String::toUpperCase)
      .map(BlastLinkSettings.AuthType::valueOf)
      .ifPresent(builder::authType);

    Optional.ofNullable(customSettings.get(BLAST_INCOMING_TOKEN_ISSUER))
      .map(Object::toString)
      .map(HttpUrl::parse)
      .ifPresent(builder::tokenIssuer);

    Optional.ofNullable(customSettings.get(BLAST_INCOMING_TOKEN_AUDIENCE))
      .map(Object::toString)
      .map(HttpUrl::parse)
      .ifPresent(builder::tokenAudience);

    Optional.ofNullable(customSettings.get(BLAST_INCOMING_SHARED_SECRET))
      .map(Object::toString)
      .ifPresent(builder::encryptedTokenSharedSecret);

    return builder;
  }

  /**
   * <p>The minimum amount of time (in milliseconds) to budget for receiving a response message from an account.</p>
   *
   * <p>Especially useful for ILP packets, if a packet expires in 30 seconds, then a link should only wait 29 seconds
   * before timing out so that it can generally be sure to reject the request (as opposed to merely allowing a timeout
   * to occur, because timeouts are ambiguous).</p>
   *
   * @return A {@link Duration}.
   */
  default Duration minMessageWindow() {
    return Duration.of(1000, ChronoUnit.MILLIS);
  }

  @Value.Immutable
  @Modifiable
  abstract class AbstractIncomingLinkSettings implements IncomingLinkSettings {

    @Override
    @Value.Default
    public Duration minMessageWindow() {
      return Duration.of(1000, ChronoUnit.MILLIS);
    }

    @Override
    @Value.Redacted
    public abstract String encryptedTokenSharedSecret();

  }
}
