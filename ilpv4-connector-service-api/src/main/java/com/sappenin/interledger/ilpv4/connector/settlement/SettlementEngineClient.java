package com.sappenin.interledger.ilpv4.connector.settlement;

import org.interledger.connector.accounts.AccountId;
import org.interledger.ilpv4.connector.core.settlement.SettlementQuantity;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

/**
 * Defines a client that can interact with a Settlement Engine.
 */
public interface SettlementEngineClient {

  /**
   * @param accountId                 The {@link AccountId} of the Connector account this request is being executed on
   *                                  behalf of.
   * @param settlementEngineAccountId The identifier that a Connector uses to correlate a Connector Account to the
   *                                  account in the Settlement Engine. For example, a Connector with accountId of `123`
   *                                  might use a settlementEngineAccountId of `peer.settle.123`. Alternatively, it
   *                                  might re-used the same identifier if the Settlement Engine only supports a single
   *                                  currency.
   */
  void createSettlementAccount(AccountId accountId, String settlementEngineAccountId);

  /**
   * Send a request to the Settlement engine to initiate a settlement payment.
   *
   * @param accountId
   * @param idempotencyKey
   * @param settlementQuantity A {@link SettlementQuantity} (in settlement engine units) that represents how much to
   *                           settle.
   *
   * @return
   */
  ResponseEntity<Resource> initiateSettlement(
    AccountId accountId, UUID idempotencyKey, SettlementQuantity settlementQuantity
  );

  /**
   * Given a message from a Peer's settlement engine, forward it to the local settlement engine by making an HTTP Post
   * request to `{settlementEngineBaseUrl}/{accounId}/message`, and then return the response.
   *
   * @param accountId
   * @param data      A byte array of data that two Settlement Engines can understand, but for which this client views
   *                  as opaque data.
   *
   * @return An opaque byte array response destined for the peer's settlment engine.
   *
   * @throws SettlementEngineClientException if the message is not accepted by the settlement engine for any reason.
   */
  byte[] sendMessageFromPeer(AccountId accountId, byte[] data) throws SettlementEngineClientException;
}
