package org.interledger.connector.ccp;

import com.google.common.base.Preconditions;
import org.immutables.value.Value;
import org.interledger.core.InterledgerAddressPrefix;

import java.util.Base64;
import java.util.Collection;
import java.util.Collections;

/**
 * A <tt>Route</tt> is an advertisement by a connector, stating that it can reach a particular destination address.
 */
@Value.Immutable
public abstract class CcpNewRoute {

  private static final byte[] EMPTY_AUTH = new byte[32];

  /**
   * The address prefix of this new, added route.
   */
  public abstract InterledgerAddressPrefix prefix();

  /**
   * A collection of 32-bytes used to authenticate the route. Reserved for the future, currently not used.
   */
  @Value.Default
  public byte[] auth() {
    return EMPTY_AUTH;
  }

  /**
   * Access {@link #auth()} as a base64-encoded String.
   */
  @Value.Derived
  public String authBase64() {
    return Base64.getEncoder().encodeToString(auth());
  }

  /**
   * The hops that would need to be taken in order to reach the prefix found in {@link #prefix()}.
   */
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
