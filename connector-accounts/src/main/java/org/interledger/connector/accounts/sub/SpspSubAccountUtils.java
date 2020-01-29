package org.interledger.connector.accounts.sub;

import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerAddress;

/**
 * A helper to centralize determination of sub-account related business logic.
 */
public interface SpspSubAccountUtils extends SubAccountUtils {

  /**
   * Determines if the supplied packet destination address should be fulfilled locally by this connector (as opposed to
   * forwarded on an outgoing link).
   *
   * @param destinationAddress A destination {@link InterledgerAddress} taken from an incoming ILP Prepare packet.
   *
   * @return {@code true} if this address should be locally fulfilled by an instance of StreamReceiver or {@code false}
   *   otherwise (i.e., if packets destined for this address should be forwarded normally).
   */
  boolean shouldFulfilLocally(InterledgerAddress destinationAddress);

  /**
   * Given an {@link InterledgerAddress}, parse out the segment that corresponds to the SPSP Account identifier. For
   * example, given a Connector operating address of `g.connector`, then locally fulfilled SPSP payments should be
   * addresses to something like `g.connector.{accountId}.{spsp_info}`. In this case, the `{accountId}` is the value
   * returned by this method.
   *
   * @param interledgerAddress An {@link InterledgerAddress} to parse.
   *
   * @return An {@link AccountId} for the supplied address.
   */
  AccountId parseSpspAccountId(InterledgerAddress interledgerAddress);
}
