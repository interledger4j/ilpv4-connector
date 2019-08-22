package org.interledger.connector.server.spring.auth.blast;

import org.interledger.connector.server.spring.auth.blast.ImmutableAuthenticationRequest;
import org.immutables.value.Value;
import org.interledger.connector.accounts.AccountId;
import org.springframework.security.core.Authentication;

/**
 * Contains information about an ILP-over-HTTP Authentication request.
 */
public interface AuthenticationRequest {

  static ImmutableAuthenticationRequest.Builder builder() {
    return ImmutableAuthenticationRequest.builder();
  }

  default AccountId principal() {
    return AccountId.of(incomingAuthentication().getPrincipal().toString());
  }

  /**
   * The incoming {@link Authentication} as supplied by the request sent by a caller trying to authenticate.
   *
   * @return The incoming {@link Authentication}
   */
  Authentication incomingAuthentication();

  /**
   * Exists in order for the principal to be used as the hashcode source for the cache.
   */
  @Value.Immutable(prehash = true)
  abstract class AbstractAuthenticationRequest implements AuthenticationRequest {

    @Override
    @Value.Derived
    public AccountId principal() {
      return AccountId.of(incomingAuthentication().getPrincipal().toString());
    }

    @Override
    public abstract Authentication incomingAuthentication();

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      AuthenticationRequest.AbstractAuthenticationRequest that =
        (AuthenticationRequest.AbstractAuthenticationRequest) o;

      return principal().equals(that.principal());

    }

    @Override
    public int hashCode() {
      return principal().hashCode();
    }
  }
}
