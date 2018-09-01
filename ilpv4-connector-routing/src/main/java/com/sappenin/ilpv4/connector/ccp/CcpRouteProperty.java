package com.sappenin.ilpv4.connector.ccp;

import com.google.common.base.Preconditions;
import org.immutables.value.Value;

/**
 * <p>A route-property is used to send meta-data about a route. Properties can be optional or required. Required
 * properties are always transitive.</p>
 *
 * <p>However, if a property is optional, it may still be transitive, and it may also be a partial property.</p>
 *
 * <p>Note that  if a property is required (i.e., non-optional) then it will always be transitive, and never
 * partial.</p>
 *
 * <p>Property values may also contain a UTF-8 String, or a sequence of bytes.</p>
 */
@Value.Immutable
public abstract class CcpRouteProperty {

  // private-access so no external caller can update these values (without resorting to reflection).
  private static final byte[] EMPTY_VALUE = new byte[0];

  /**
   * The unique identifier of this property.
   */
  public abstract int id();

  /**
   * The value of this property. If {@link #utf8()} is {@code true}, then these bytes can be decoded to a String.
   * Otherwise, the value should be considered as a simple buffer of bytes.
   */
  @Value.Default
  public byte[] value() {
    // Returns an array that doesn't influence the original array.
    return EMPTY_VALUE.clone();
  }

  @Value.Default
  public boolean optional() {
    return false;
  }

  @Value.Default
  public boolean transitive() {
    return true;
  }

  /**
   * Determines if this route contains only partial information about the entire route.
   */
  @Value.Default
  public boolean partial() {
    return false;
  }

  /**
   * Indicates if {@link #value()} is a UTF-8 String.
   *
   * @return <tt>true</tt> if {@link #value()} is a UTF-8 {@link String}; <tt>false</tt> otherwise.
   */
  @Value.Default
  public boolean utf8() {
    return true;
  }

  @Value.Check
  protected void check() {
    if (optional() == false) {
      Preconditions.checkArgument(transitive(), "Transitive bit must be set for well-known properties!");
    }
  }
}
