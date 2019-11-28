package org.interledger.connector.core;

import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerFulfillment;

/**
 * @deprecated Remove in-favor of {@link org.interledger.core.InterledgerConstants}.
 */
@Deprecated
public class Ilpv4Constants {

  // An empty fulfillment, used in various scenarios peer-wise scenarios where the condition/fulfillment is
  // unimportant for processing an ILP packet (e.g., sending settlement messages).
  @Deprecated
  public static final InterledgerFulfillment ALL_ZEROS_FULFILLMENT = InterledgerFulfillment.of(new byte[32]);
  @Deprecated
  public static final InterledgerCondition ALL_ZEROS_CONDITION = ALL_ZEROS_FULFILLMENT.getCondition();


}
