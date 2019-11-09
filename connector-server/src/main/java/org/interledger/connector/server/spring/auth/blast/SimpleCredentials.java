package org.interledger.connector.server.spring.auth.blast;

import org.interledger.connector.accounts.AccountId;

import com.auth0.jwt.JWT;
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

  /**
   * See {@link #isAuthenticated()} for a full description.
   * <p>
   * Implementations should <b>always</b> allow this method to be called with a
   * <code>false</code> parameter, as this is used by various classes to specify the
   * authentication token should not be trusted. If an implementation wishes to reject an invocation with a
   * <code>true</code> parameter (which would indicate the authentication token is trusted - a potential security risk)
   * the implementation should throw an {@link IllegalArgumentException}.
   *
   * @param isAuthenticated <code>true</code> if the token should be trusted (which may
   *                        result in an exception) or <code>false</code> if the token should not be trusted
   *
   * @throws IllegalArgumentException if an attempt to make the authentication token trusted (by passing
   *                                  <code>true</code> as the argument) is rejected due to the implementation being
   *                                  immutable or implementing its own alternative approach to {@link
   *                                  #isAuthenticated()}
   */
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
