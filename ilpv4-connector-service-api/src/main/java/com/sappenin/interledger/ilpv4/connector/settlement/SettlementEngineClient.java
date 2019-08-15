package com.sappenin.interledger.ilpv4.connector.settlement;

import okhttp3.HttpUrl;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.SettlementEngineAccountId;
import org.interledger.ilpv4.connector.settlement.SettlementEngineClientException;
import org.interledger.ilpv4.connector.settlement.client.CreateSettlementAccountRequest;
import org.interledger.ilpv4.connector.settlement.client.CreateSettlementAccountResponse;
import org.interledger.ilpv4.connector.settlement.client.InitiateSettlementRequest;
import org.interledger.ilpv4.connector.settlement.client.InitiateSettlementResponse;
import org.interledger.ilpv4.connector.settlement.client.SendMessageRequest;
import org.interledger.ilpv4.connector.settlement.client.SendMessageResponse;

import java.util.UUID;

/**
 * Defines a client that can interact with a Settlement Engine.
 */
public interface SettlementEngineClient {

  /**
   * Create a settlement account in the settlement engine.
   *
   * @param accountId                      The {@link AccountId} of the Connector account this request is being executed
   *                                       on behalf of.
   * @param endpointUrl                    A {@link HttpUrl} for the settlement engine. This value is in this API
   *                                       contract because this interface merely provides a type-safe implementation
   *                                       over the underlying client, which will manage HTTP connections internally
   *                                       based upon http host.
   * @param createSettlementAccountRequest The identifier that a Connector uses to correlate a Connector Account to the
   *                                       account in the Settlement Engine. For example, a Connector with accountId of
   *                                       `123` might use a settlementEngineAccountId of `peer.settle.123`.
   *                                       Alternatively, it might re-used the same identifier for convenience.
   *
   * @return A {@link CreateSettlementAccountResponse}
   *
   * @throws SettlementEngineClientException if the account is unable to be created.
   */
  CreateSettlementAccountResponse createSettlementAccount(
    AccountId accountId,
    HttpUrl endpointUrl,
    CreateSettlementAccountRequest createSettlementAccountRequest
  ) throws SettlementEngineClientException;

  /**
   * Send a request to the settlement engine to initiate a settlement payment.
   *
   * @param accountId                 The {@link AccountId} of the Router account making this request.
   * @param settlementEngineAccountId The {@link SettlementEngineAccountId} for the Router account making this request.
   *                                  Note that some settlement engines will yield a settlementAccountId during account
   *                                  creation on the settlement engine, so this field allows this value to diverge from
   *                                  {@code accountId}.
   * @param idempotencyKey
   * @param endpointUrl               A {@link HttpUrl} for the settlement engine. This value is in this API contract
   *                                  because this interface merely provides a type-safe implementation over the
   *                                  underlying client, which will manage HTTP connections internally based upon http
   *                                  host.
   * @param initiateSettlementRequest A {@link InitiateSettlementRequest} (in settlement engine units) that represents
   *                                  how much to settle.
   *
   * @return A {@link InitiateSettlementResponse}.
   *
   * @throws SettlementEngineClientException if the settlement is not able to be initiated.
   */
  InitiateSettlementResponse initiateSettlement(
    AccountId accountId,
    SettlementEngineAccountId settlementEngineAccountId,
    UUID idempotencyKey,
    HttpUrl endpointUrl,
    InitiateSettlementRequest initiateSettlementRequest
  ) throws SettlementEngineClientException;

  /**
   * Given a message from a Peer's settlement engine, forward it to the local settlement engine by making an HTTP Post
   * request to `{settlementEngineBaseUrl}/{accounId}/message`, and then return the response.
   *
   * @param accountId                 The {@link AccountId} of the account this request is being executed on behalf of.
   * @param settlementEngineAccountId The {@link SettlementEngineAccountId} for the Router account making this request.
   *                                  Note that some settlement engines will yield a settlementAccountId during account
   *                                  creation on the settlement engine, so this field allows this value to diverge from
   *                                  {@code accountId}.
   * @param endpointUrl               A {@link HttpUrl} for the settlement engine. This value is in this API contract
   *                                  because this interface merely provides a type-safe implementation over the
   *                                  underlying client, which will manage HTTP connections internally based upon http
   *                                  host.
   * @param sendMessageRequest        A {@link SendMessageRequest} containing everything needed to send a message to the
   *                                  Settlement Engine of this account's counterparty.
   *
   * @return An opaque byte array response destined for the peer's settlment engine.
   *
   * @throws SettlementEngineClientException if the message is not accepted by the settlement engine for any reason.
   */
  SendMessageResponse sendMessageFromPeer(
    AccountId accountId, SettlementEngineAccountId settlementEngineAccountId, HttpUrl endpointUrl,
    SendMessageRequest sendMessageRequest
  ) throws SettlementEngineClientException;
}
