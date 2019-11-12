package org.interledger.connector.server.spring.auth.blast;

import org.interledger.connector.accounts.AccountId;

import com.google.common.collect.Lists;
import com.google.common.hash.HashCode;
import org.immutables.value.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Contains information about an ILP-over-HTTP Authentication decision, generally used for caching purposes to shield
 * any actual underlying shared-secrets from memory.
 */
@Value.Immutable
public interface AuthenticationDecision extends Authentication {

  static ImmutableAuthenticationDecision.Builder builder() {
    return ImmutableAuthenticationDecision.builder();
  }

  /**
   * The principal that this Authentication decision represents. Empty if not authenticated
   *
   * @return The principal that the original authentication request was attempting to authenticate.
   */
  @Override
  Optional<AccountId> getPrincipal();

  /**
   * An HMAC of the original credential, for comparison purposes.
   *
   * @return The HMAC of a credential.
   */
  @Value.Redacted
  HashCode credentialHmac();

  @Override
  @Value.Default()
  default boolean isAuthenticated() {
    return false;
  }

  @Override
  @Value.Derived
  default Collection<? extends GrantedAuthority> getAuthorities() {
    return Lists.newArrayList();
  }

  @Override
  @Value.Derived
  default Object getCredentials() {
    return this.credentialHmac();
  }

  /**
   * Stores additional details about the authentication request. These might be an IP address, certificate serial number
   * etc.
   *
   * @return additional details about the authentication request, or <code>null</code> if not used
   */
  @Override
  @Value.Derived
  @Nullable
  default Object getDetails() {
    return null;
  }

  @Override
  @Value.Derived
  default void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
    // No-op.
  }

  /**
   * Returns the name of this principal.
   *
   * @return the name of this principal.
   */
  @Override
  @Value.Default
  default String getName() {
    return Optional.ofNullable(getPrincipal())
        .map(Object::toString)
        .orElse(null);
  }

}
