package org.interledger.ilpv4.connector.core.settlement;

import okhttp3.HttpUrl;
import org.immutables.value.Value;

/**
 * Contains all details relating to a Settlement Engine.
 *
 * @see "https://interledger.org/rfcs/FIXME" //TODO: replace with finalized RFC link.
 */
public interface SettlementEngineDetails {

  static ImmutableSettlementEngineDetails.Builder builder() {
    return ImmutableSettlementEngineDetails.builder();
  }

  /**
   * The base URL of the settlement engine.
   *
   * @return An {@link HttpUrl} for a Settlement Engine.
   */
  HttpUrl baseUrl();

  /**
   * The order of magnitude used to express one full asset unit in the base units of the underlying account. More
   * formally, an integer (..., -2, -1, 0, 1, 2, ...), such that one full unit of a particular asset equals 10^(-scale)
   * units in the scaled asset of the underlying account.
   *
   * For example, using the U.S. Dollar asset, one full unit is just one dollar. However, an account might prefer to
   * track its full units using some other scale, such as "cents". In this case, the account's scale would be 2. In
   * other words, 1 full unit (of dollars) is equal to 10^-2 account units, or 100 account units, or 100 cents.
   *
   * 100 account units (i.e., 100 cents) can be translated into dollars (i.e., 1 base unit) via the following equation:
   * (100 cents) * (10^(-2)) = (1 dollar).
   *
   * @return The scale of this Quantity, relative to some underlying base unit of account.
   */
  int assetScale();

  @Value.Immutable
  abstract class AbstractSettlementEngineDetails implements SettlementEngineDetails {

  }
}
