package org.interledger.connector.link.blast;

import org.immutables.value.Value;

import java.time.Instant;

/**
 * Contains information about an ILP-over-HTTP Authentication decision, generally used for caching purposes to sheild
 * any actual underlying shared-secrets from memory.
 */
@Value
public interface AuthenticationDecision {

  /**
   * The {@link Instant} this authentication decision was created.
   *
   * @return The {@link Instant}.
   */
  @Value.Default
  default Instant creationDateTime() {
    return Instant.now();
  }

  /**
   * The principal that this Authentication decision represents. For now, this is an accountId in String-form.
   *
   * @return The principal that the oringal authentication request was attempting to authenticate.
   */
  String principal();

  /**
   * An HMAC of the original credential, for comparison purposes.
   *
   * @return The HMAC of a credential.
   */
  @Value.Redacted
  byte[] credentialHmac();

  /**
   * The authentication decision, as a boolean.
   *
   * @return {@code true} if the principal in {@link #principal()} was successfully authenticated; {@code false} if the
   * principal was not successfully authenticated.
   */
  boolean isAuthenticated();

}
