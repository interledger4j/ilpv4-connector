package org.interledger.connector.link.blast;

import okhttp3.HttpUrl;
import org.immutables.value.Value;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Modifiable;
import org.interledger.connector.link.LinkSettings;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public interface BlastLinkSettings extends LinkSettings {

  String DOT = ".";
  String BLAST = "blast";

  String OUTGOING = "outgoing";
  String INCOMING = "incoming";

  String SECRET = "secret";
  String TOKEN_EXPIRY = "token_expiry";
  String URL = "url";

  String BLAST_INCOMING_SECRET = BLAST + DOT + INCOMING + DOT + SECRET;
  String BLAST_OUTGOING_SECRET = BLAST + DOT + OUTGOING + DOT + SECRET;
  String BLAST_OUTGOING_URL = BLAST + DOT + OUTGOING + DOT + URL;
  String BLAST_OUTGOING_TOKEN_EXPIRY = BLAST + DOT + OUTGOING + DOT + TOKEN_EXPIRY;

  static ImmutableBlastLinkSettings.Builder builder() {
    return ImmutableBlastLinkSettings.builder();
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

    // When loaded from a properties file, the properties are hierarchical in a Map. However, in Java, they are not, so
    // consider both options. Generally only one will be present, but if for some reason both are present, the String
    // values will win.
    Optional.ofNullable(customSettings.get(BLAST))
      .map(val -> (Map<String, Object>) val)
      .ifPresent(blastSettings -> {

        Optional.ofNullable(blastSettings.get(INCOMING))
          .map(val -> (Map<String, Object>) val)
          .ifPresent(incomingSettings -> {
            Optional.ofNullable(incomingSettings.get(SECRET))
              .map(Object::toString)
              .ifPresent(builder::incomingSecret);
          });

        Optional.ofNullable(blastSettings.get(OUTGOING))
          .map(val -> (Map<String, Object>) val)
          .ifPresent(outgoingSettings -> {

            Optional.ofNullable(outgoingSettings.get(SECRET))
              .map(Object::toString)
              .ifPresent(builder::outgoingSecret);

            Optional.ofNullable(outgoingSettings.get(TOKEN_EXPIRY))
              .map(Object::toString)
              .map(Duration::parse)
              .ifPresent(builder::outingTokenExpiry);

            Optional.ofNullable(outgoingSettings.get(URL))
              .map(Object::toString)
              .map(HttpUrl::parse)
              .ifPresent(builder::outgoingUrl);

          });
      });

    Optional.ofNullable(customSettings.get(BLAST_INCOMING_SECRET))
      .map(Object::toString)
      .ifPresent(builder::incomingSecret);

    Optional.ofNullable(customSettings.get(BLAST_OUTGOING_SECRET))
      .map(Object::toString)
      .ifPresent(builder::outgoingSecret);

    Optional.ofNullable(customSettings.get(BLAST_OUTGOING_TOKEN_EXPIRY))
      .map(Object::toString)
      .map(Duration::parse)
      .ifPresent(builder::outingTokenExpiry);

    Optional.ofNullable(customSettings.get(BLAST_OUTGOING_URL))
      .map(Object::toString)
      .map(HttpUrl::parse)
      .ifPresent(builder::outgoingUrl);

    return builder;
  }

  /**
   * A simple `secret` used to authenticate an incoming BLAST connection, useful for development purposes. For
   * production, a more advanced authentication mechanism should be used.
   *
   * @return
   */
  String getIncomingSecret();

  /**
   * A simple `secret` used to authenticate to the remote peer when making a BLAST connection, useful for development
   * purposes. For production, a more advanced authentication mechanism should be used.
   *
   * @return
   */
  String getOutgoingSecret();

  /**
   * Determines how often to sign a new token for auth.
   *
   * @return
   */
  Duration getOutingTokenExpiry();

  /**
   * endpoint to POST packets to. If url contains a percent and the link is in `multi` mode, then the segment after this
   * link's own address will be filled where the `%` is  when routing packets.
   *
   * @return
   */
  HttpUrl getOutgoingUrl();

  /**
   * <p>The minimum amount of time (in milliseconds) to budget for receiving a response message from an account.</p>
   *
   * <p>Especially useful for ILP packets, if a packet expires in 30 seconds, then a link should only wait 29 seconds
   * before timing out so that it can generally be sure to reject the request (as opposed to merely allowing a timeout
   * to occur, because timeouts are ambiguous).</p>
   *
   * @return A {@link Duration}.
   */
  default Duration getMinMessageWindow() {
    return Duration.of(1000, ChronoUnit.MILLIS);
  }

  @Value.Immutable
  @Modifiable
  abstract class AbstractBlastLinkSettings implements BlastLinkSettings {

    @Override
    @Default
    public Duration getMinMessageWindow() {
      return Duration.of(1000, ChronoUnit.MILLIS);
    }

  }
}
