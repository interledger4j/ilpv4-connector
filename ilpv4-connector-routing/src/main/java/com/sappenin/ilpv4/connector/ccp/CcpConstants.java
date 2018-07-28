package com.sappenin.ilpv4.connector.ccp;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerFulfillment;

import java.util.Base64;

public final class CcpConstants {

  public static final InterledgerAddress CCP_CONTROL_DESTINATION = InterledgerAddress.of("peer.route.control");
  public static final InterledgerAddress CCP_UPDATE_DESTINATION = InterledgerAddress.of("peer.route.update");

  // Fulfilment with Zero-filled byte array of 32 bytes.
  public static final InterledgerFulfillment PEER_PROTOCOL_FULFILLMENT = InterledgerFulfillment.of(new byte[32]);
  public static final InterledgerCondition PEER_PROTOCOL_CONDITION = InterledgerCondition.of(Base64.getDecoder().decode
    ("Zmh6rfhivXdsj8GLjp+OIAiXFIVu4jOzkCpZHQ1fKSU="));

  public static final int PEER_PROTOCOL_EXPIRY_DURATION = 60000;
}
