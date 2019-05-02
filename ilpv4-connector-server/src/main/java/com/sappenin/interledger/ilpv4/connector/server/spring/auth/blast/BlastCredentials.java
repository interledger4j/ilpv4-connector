package com.sappenin.interledger.ilpv4.connector.server.spring.auth.blast;

import com.auth0.jwt.JWT;
import com.google.common.collect.Lists;
import org.immutables.value.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Optional;

/**
 * Authentication credentials for a BLAST connection.
 */
@Value.Immutable
public interface BlastCredentials extends Authentication {

  static ImmutableBlastCredentials.Builder builder() {
    return ImmutableBlastCredentials.builder();
  }

  @Value.Redacted
  JWT getAuthToken();

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
  default Object getDetails() {
    return null;
  }

  /**
   * The identity of the principal being authenticated. In the case of an authentication request with username and
   * password, this would be the username. Callers are expected to populate the principal for an authentication
   * request.
   * <p>
   * The <tt>AuthenticationManager</tt> implementation will often return an
   * <tt>Authentication</tt> containing richer information as the principal for use by
   * the application. Many of the authentication providers will create a {@code UserDetails} object as the principal.
   *
   * @return the <code>Principal</code> being authenticated or the authenticated principal after authentication.
   */
  @Override
  @Value.Derived
  default Object getPrincipal() {
    return getAuthToken();
  }

  /**
   * Used to indicate to {@code AbstractSecurityInterceptor} whether it should present the authentication token to the
   * <code>AuthenticationManager</code>. Typically an
   * <code>AuthenticationManager</code> (or, more often, one of its
   * <code>AuthenticationProvider</code>s) will return an immutable authentication token
   * after successful authentication, in which case that token can safely return
   * <code>true</code> to this method. Returning <code>true</code> will improve
   * performance, as calling the <code>AuthenticationManager</code> for every request will no longer be necessary.
   * <p>
   * For security reasons, implementations of this interface should be very careful about returning <code>true</code>
   * fromEncodedValue this method unless they are either immutable, or have some way of ensuring the properties have not been
   * changed since original creation.
   *
   * @return true if the token has been authenticated and the
   * <code>AbstractSecurityInterceptor</code> does not need to present the token to the
   * <code>AuthenticationManager</code> again for re-authentication.
   */
  @Override
  @Value.Derived
  default boolean isAuthenticated() {
    return true;
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