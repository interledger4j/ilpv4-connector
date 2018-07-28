package com.sappenin.ilpv4.connector.ccp;

import com.google.common.base.Preconditions;
import org.immutables.value.Value;
import org.interledger.core.InterledgerAddress;

import java.util.Base64;
import java.util.Collection;
import java.util.Collections;

@Value.Immutable
public abstract class CcpRoute {

  public abstract InterledgerAddress prefix();

  /**
   * A collection of 32-bytes used to authenticate the route.
   */
  public abstract byte[] auth();

  /**
   * Access {@link #auth()} as a base64-encoded String.
   */
  @Value.Derived
  public String authBase64() {
    return Base64.getEncoder().encodeToString(auth());
  }

  @Value.Default
  public Collection<CcpRoutePathPart> path() {
    return Collections.emptyList();
  }

  @Value.Default
  public Collection<CcpRouteProperty> properties() {
    return Collections.emptyList();
  }

  @Value.Check
  protected void check() {
    Preconditions.checkState(auth().length == 32, "route 'auth' must be exactly 32 bytes!");
  }
}
