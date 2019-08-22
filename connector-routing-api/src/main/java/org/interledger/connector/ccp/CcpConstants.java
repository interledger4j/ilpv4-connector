package org.interledger.connector.ccp;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerFulfillment;

import java.util.Base64;

public final class CcpConstants {

  public static final String CCP_CONTROL_DESTINATION = "peer.route.control";
  public static final String CCP_UPDATE_DESTINATION = "peer.route.update";

  public static final InterledgerAddress CCP_CONTROL_DESTINATION_ADDRESS =
    InterledgerAddress.of(CCP_CONTROL_DESTINATION);
  public static final InterledgerAddress CCP_UPDATE_DESTINATION_ADDRESS = InterledgerAddress.of(CCP_UPDATE_DESTINATION);

  // Fulfilment with Zero-filled byte array of 32 bytes.
  public static final InterledgerFulfillment PEER_PROTOCOL_EXECUTION_FULFILLMENT
    = InterledgerFulfillment.of(new byte[32]);
  public static final InterledgerCondition PEER_PROTOCOL_EXECUTION_CONDITION
    = InterledgerCondition.of(Base64.getDecoder().decode("Zmh6rfhivXdsj8GLjp+OIAiXFIVu4jOzkCpZHQ1fKSU="));
}
