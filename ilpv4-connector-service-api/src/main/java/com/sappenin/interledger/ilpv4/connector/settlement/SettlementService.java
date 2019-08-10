package com.sappenin.interledger.ilpv4.connector.settlement;

import com.sappenin.interledger.ilpv4.connector.balances.BalanceTrackerException;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.SettlementEngineAccountId;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.ilpv4.connector.core.settlement.SettlementQuantity;
import org.interledger.ilpv4.connector.settlement.SettlementServiceException;

import java.util.UUID;

/**
 * A service for handling interactions with the Settlement Service that allows a separation of HTTP caching and
 * idempotence from actual settlement logic.
 */
public interface SettlementService {

  /**
   * Handle an incoming Settlement payment as advertised by the local Settlement Engine. This payment indicated that the
   * local Settlement Engine has detected an on-ledger incoming payment destined for a particular account (indicated by
   * {@code accountId}) operated by this connector.
   *
   * @param idempotencyKey                      A unique key to ensure idempotent requests.
   * @param settlementEngineAccountId           The {@link SettlementEngineAccountId} as supplied by the settlement
   *                                            engine. Note that settlement engines could theoretically store any type
   *                                            of identifier(either supplied by the Connector, or created by the
   *                                            engine). Thus, this implementation simply uses the settlement engines
   *                                            view of the world (i.e., a {@link SettlementEngineAccountId}) for
   *                                            communication.
   * @param incomingSettlementInSettlementUnits A {@link SettlementQuantity} that reflects the amount of units that were
   *                                            settled (in settlement units).
   *
   * @return A new {@link SettlementQuantity} that indicates how much was settled inside of this Connector's ILP
   * settlement layer.
   *
   * @throws BalanceTrackerException If anything goes wrong while attempting to update the clearingBalance.
   */
  SettlementQuantity onLocalSettlementPayment(
    String idempotencyKey, SettlementEngineAccountId settlementEngineAccountId,
    SettlementQuantity incomingSettlementInSettlementUnits
  ) throws BalanceTrackerException;

  /**
   * Handle a generic message from the Settlement Engine servicing a particular account.
   *
   * Note that this message was likely received on the Account Link that is used by the account identified by {@code
   * accountId}.
   *
   * @param settlementEngineAccountId The {@link SettlementEngineAccountId} as supplied by the settlement engine. Note
   *                                  that settlement engines could theoretically store any type of identifier(either
   *                                  supplied by the Connector, or created by the engine). Thus, this implementation
   *                                  simply uses the settlement engines view of the world (i.e., a {@link
   *                                  SettlementEngineAccountId}) for communication.
   * @param message                   An opaque binary message that will be wrapped in an ILPv4 packet and routed to the
   *                                  correct peer for the the supplied {@code accountId} for further processing.
   *
   * @return A byte-array containing opaque binary data as receieved from the peer's settlement engine (Note that it is
   * recommended per the RFC to communicate with the peer's settlment engine using ILP links, so this information SHOULD
   * have come from inside of an ILP response packet).
   */
  byte[] onLocalSettlementMessage(SettlementEngineAccountId settlementEngineAccountId, byte[] message);

  /**
   * Handle an incoming ILP Prepare packet containing a settlement message that was proxied via the counterparty
   * connected to this Connector via the account represented by {@code accountSettings}.
   *
   * An example of such a packet is when Alice's Settlement Engine emits a message related to a payment channel. This
   * message is sent to Alice's Connector, which then wraps the message in an ILP Prepare packet with a destination of
   * `peer.settle` and deposits the packet on a particular account link. Once transmitted to the peer'd connector, the
   * message ends up in this method.
   *
   * @param accountSettings                 The {@link AccountSettings} for the account that this settlement message
   *                                        came in over.
   * @param messageFromPeerSettlementEngine A byte-array that contains an opaque binary message that should be delivered
   *                                        to the local settlement engine configured for this account.
   *
   * @return An {@link InterledgerResponsePacket} with a proper response destined for the peer's Settlement Engine.
   */
  byte[] onSettlementMessageFromPeer(
    AccountSettings accountSettings, byte[] messageFromPeerSettlementEngine
  );

  /**
   * Communicate with the appropriate settlement engine to initiate a settlement payment.
   *
   * @param idempotencyKey                    A {@link UUID} used for idempotency.
   * @param accountSettings                   An {@link AccountSettings} that identifies the account to settle.
   * @param settlementQuantityInClearingUnits A {@link SettlementQuantity} in clearing-layer units so that this service
   *                                          can deal only with settlement-layer units.
   *
   * @return A {@link SettlementQuantity} in the units of the clearing system.
   *
   * @throws SettlementServiceException if the settlement cannot be initiated for any reason.
   */
  SettlementQuantity initiateSettlementForFulfillThreshold(
    UUID idempotencyKey, AccountSettings accountSettings, SettlementQuantity settlementQuantityInClearingUnits
  ) throws SettlementServiceException;
}
