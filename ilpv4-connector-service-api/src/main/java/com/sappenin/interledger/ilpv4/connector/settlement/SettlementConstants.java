package com.sappenin.interledger.ilpv4.connector.settlement;

import org.interledger.core.InterledgerAddress;

public interface SettlementConstants {

  String IDEMPOTENCY_KEY = "Idempotency-Key";

  String ACCOUNTS = "accounts";

  String MESSAGES = "messages";
  String SETTLEMENTS = "settlement";

  String PEER_DOT_SETTLE_STRING = "peer.settle";
  InterledgerAddress PEER_DOT_SETTLE = InterledgerAddress.of(PEER_DOT_SETTLE_STRING);

}
