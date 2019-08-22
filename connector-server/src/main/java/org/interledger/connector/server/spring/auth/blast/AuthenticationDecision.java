package org.interledger.connector.server.spring.auth.blast;

import org.interledger.connector.server.spring.auth.blast.ImmutableAuthenticationDecision;
import org.immutables.value.Value;
import org.interledger.connector.accounts.AccountId;
import org.springframework.security.core.Authentication;

/**
 * Contains information about an ILP-over-HTTP Authentication decision, generally used for caching purposes to shield
 * any actual underlying shared-secrets from memory.
 */
@Value.Immutable
public interface AuthenticationDecision {

  static ImmutableAuthenticationDecision.Builder builder() {
    return ImmutableAuthenticationDecision.builder();
  }

  /**
   * The principal that this Authentication decision represents.
   *
   * @return The principal that the original authentication request was attempting to authenticate.
   */
  @Value.Derived
  default AccountId principal() {
    return AccountId.of(authentication().getPrincipal().toString());
  }

  /**
   * The {@link Authentication} object that will actually be returned from the cache. Note that this object will _not_
   * contain an actual credential.
   *
   * @return An {@link Authentication}.
   */
  @Value.Redacted
  Authentication authentication();

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
  @Value.Derived
  default boolean isAuthenticated() {
    return authentication().isAuthenticated();
  }

}
