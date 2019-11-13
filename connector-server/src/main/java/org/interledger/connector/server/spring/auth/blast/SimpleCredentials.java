package org.interledger.connector.server.spring.auth.blast;

import org.interledger.connector.accounts.AccountId;

import com.google.common.collect.Lists;
import org.immutables.value.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Authentication credentials for a simple auth (username:base64-encoded-secret)
 */
@Value.Immutable
public interface SimpleCredentials extends Authentication {

  static ImmutableSimpleCredentials.Builder builder() {
    return ImmutableSimpleCredentials.builder();
  }

  @Override
  AccountId getPrincipal();

  @Override
  @Value.Default()
  default boolean isAuthenticated() {
    return false;
  }

  @Value.Redacted
  byte[] getAuthToken();

  @Override
  @Value.Derived
  default Collection<? extends GrantedAuthority> getAuthorities() {
    return Lists.newArrayList();
  }

  @Override
  @Value.Derived
  default Object getCredentials() {
    return this.getAuthToken();
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
