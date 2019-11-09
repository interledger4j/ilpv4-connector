package org.interledger.connector.metrics;

import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.events.IncomingSettlementFailedEvent;
import org.interledger.connector.events.IncomingSettlementSucceededEvent;
import org.interledger.connector.events.OutgoingSettlementInitiationFailedEvent;
import org.interledger.connector.events.OutgoingSettlementInitiationSucceededEvent;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;

/**
 * Defines how to track various metrics in the Connector.
 */
public interface MetricsService {

  /**
   * Track an incoming packet that was prepared.
   *
   * @param accountSettings The {@link AccountSettings} for the account the packet was sent on.
   * @param preparePacket   The {@link InterledgerPreparePacket} being tracked.
   */
  void trackIncomingPacketPrepared(AccountSettings accountSettings, InterledgerPreparePacket preparePacket);

  /**
   * Track an incoming packet that was fulfilled.
   *
   * @param accountSettings The {@link AccountSettings} for the account the packet was sent on.
   * @param fulfillPacket   The {@link InterledgerFulfillPacket} being tracked.
   */
  void trackIncomingPacketFulfilled(AccountSettings accountSettings, InterledgerFulfillPacket fulfillPacket);

  /**
   * Track an incoming packet that was rejected.
   *
   * @param accountSettings The {@link AccountSettings} for the account the packet was sent on.
   * @param rejectPacket    The {@link InterledgerRejectPacket} being tracked.
   */
  void trackIncomingPacketRejected(AccountSettings accountSettings, InterledgerRejectPacket rejectPacket);

  /**
   * Track an incoming packet that failed in the Connector pipeline.
   *
   * @param accountSettings The {@link AccountSettings} for the account the packet was sent on.
   */
  void trackIncomingPacketFailed(AccountSettings accountSettings);

  /**
   * Track an outgoing packet that was prepared.
   *
   * @param accountSettings The {@link AccountSettings} for the account the packet was sent on.
   * @param preparePacket   The {@link InterledgerPreparePacket} being tracked.
   */
  void trackOutgoingPacketPrepared(AccountSettings accountSettings, InterledgerPreparePacket preparePacket);

  /**
   * Track an outgoing packet that was fulfilled.
   *
   * @param accountSettings The {@link AccountSettings} for the account the packet was sent on.
   * @param fulfillPacket   The {@link InterledgerFulfillPacket} being tracked.
   */
  void trackOutgoingPacketFulfilled(AccountSettings accountSettings, InterledgerFulfillPacket fulfillPacket);

  /**
   * Track an outgoing packet that was rejected.
   *
   * @param accountSettings The {@link AccountSettings} for the account the packet was sent on.
   * @param rejectPacket    The {@link InterledgerRejectPacket} being tracked.
   */
  void trackOutgoingPacketRejected(AccountSettings accountSettings, InterledgerRejectPacket rejectPacket);

  /**
   * Track an outgoing packet that failed in the Connector pipeline.
   *
   * @param accountSettings The {@link AccountSettings} for the account the packet was sent on.
   */
  void trackOutgoingPacketFailed(AccountSettings accountSettings);

  /**
   * Track the number of times an account was rate-limited.
   *
   * @param accountSettings The {@link AccountSettings} for the account the packet was sent on.
   */
  void trackNumRateLimitedPackets(AccountSettings accountSettings);

  /**
   * Updates Prometheus whenever an incoming settlement has been processed.
   *
   * @param event A {@link IncomingSettlementSucceededEvent} event.
   */
  void trackIncomingSettlementSucceeded(IncomingSettlementSucceededEvent event);

  /**
   * Updates Prometheus whenever an incoming settlement has failed to process.
   *
   * @param event A {@link IncomingSettlementFailedEvent} event.
   */
  void trackIncomingSettlementFailed(IncomingSettlementFailedEvent event);

  /**
   * Updates Prometheus whenever an outgoing settlement initiation has been successfully processed by the settlement
   * engine.
   *
   * @param event A {@link IncomingSettlementSucceededEvent} event.
   */
  void trackOutgoingSettlementInitiationSucceeded(final OutgoingSettlementInitiationSucceededEvent event);

  /**
   * Updates Prometheus whenever an outgoing settlement initiation has failed.
   *
   * @param event A {@link OutgoingSettlementInitiationFailedEvent} event.
   */
  void trackOutgoingSettlementInitiationFailed(final OutgoingSettlementInitiationFailedEvent event);

  /**
   * An enum that defines the valid states for a packet response.
   */
  enum PacketStatusResult {
    PREPARED,
    FULFILLED,
    REJECTED,
    FAILED
  }
}
