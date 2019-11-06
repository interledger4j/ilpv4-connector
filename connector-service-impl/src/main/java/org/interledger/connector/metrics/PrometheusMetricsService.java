package org.interledger.connector.metrics;

import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.events.IncomingSettlementFailedEvent;
import org.interledger.connector.events.IncomingSettlementSucceededEvent;
import org.interledger.connector.events.OutgoingSettlementInitiationFailedEvent;
import org.interledger.connector.events.OutgoingSettlementInitiationSucceededEvent;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;

import java.util.Objects;

/**
 * An implementation of {@link MetricsService} that emits data to Prometheus.
 */
public class PrometheusMetricsService implements MetricsService {

  private static final String EMPTY_REJECT_CODE = "";
  private static final String SETTLEMENT_SUCCEEDED = "succeeded";
  private static final String SETTLEMENT_FAILED = "failed";

  private static String stringify(final long l) {
    return l + "";
  }

  @Override
  public void trackIncomingPacketPrepared(AccountSettings accountSettings, InterledgerPreparePacket preparePacket) {
    Objects.requireNonNull(accountSettings);
    Objects.requireNonNull(preparePacket);

    // Labels: RESULT, REJECT_CODE, ACCOUNT_ID, ASSET_CODE, ASSET_SCALE
    PrometheusCollectors.incomingPackets.labels(
        PacketStatusResult.PREPARED.name(),
        EMPTY_REJECT_CODE,
        accountSettings.accountId().value(),
        accountSettings.assetCode(),
        stringify(accountSettings.assetScale())
    ).inc();
  }

  @Override
  public void trackIncomingPacketFulfilled(
      final AccountSettings accountSettings, final InterledgerFulfillPacket fulfillPacket
  ) {
    Objects.requireNonNull(accountSettings);
    Objects.requireNonNull(fulfillPacket);

    // Labels: RESULT, REJECT_CODE, ACCOUNT_ID, ASSET_CODE, ASSET_SCALE
    PrometheusCollectors.incomingPackets.labels(
        PacketStatusResult.FULFILLED.name(),
        EMPTY_REJECT_CODE,
        accountSettings.accountId().value(),
        accountSettings.assetCode(),
        stringify(accountSettings.assetScale())
    ).inc();
  }

  @Override
  public void trackIncomingPacketRejected(
      final AccountSettings accountSettings, final InterledgerRejectPacket rejectPacket
  ) {
    Objects.requireNonNull(accountSettings);
    Objects.requireNonNull(rejectPacket);

    // Labels: RESULT, REJECT_CODE, ACCOUNT_ID, ASSET_CODE, ASSET_SCALE
    PrometheusCollectors.incomingPackets.labels(
        PacketStatusResult.REJECTED.name(),
        rejectPacket.getCode().getCode(),
        accountSettings.accountId().value(),
        accountSettings.assetCode(),
        stringify(accountSettings.assetScale())
    ).inc();
  }

  @Override
  public void trackIncomingPacketFailed(final AccountSettings accountSettings) {
    Objects.requireNonNull(accountSettings);

    // Labels: RESULT, REJECT_CODE, ACCOUNT_ID, ASSET_CODE, ASSET_SCALE
    PrometheusCollectors.incomingPackets.labels(
        PacketStatusResult.FAILED.name(),
        EMPTY_REJECT_CODE,
        accountSettings.accountId().value(),
        accountSettings.assetCode(),
        stringify(accountSettings.assetScale())
    ).inc();
  }

  @Override
  public void trackOutgoingPacketPrepared(AccountSettings accountSettings, InterledgerPreparePacket preparePacket) {
    Objects.requireNonNull(accountSettings);
    Objects.requireNonNull(preparePacket);

    // Labels: RESULT, REJECT_CODE, ACCOUNT_ID, ASSET_CODE, ASSET_SCALE
    PrometheusCollectors.outgoingPackets.labels(
        PacketStatusResult.PREPARED.name(),
        EMPTY_REJECT_CODE,
        accountSettings.accountId().value(),
        accountSettings.assetCode(),
        stringify(accountSettings.assetScale())
    ).inc();
  }

  @Override
  public void trackOutgoingPacketFulfilled(
      final AccountSettings accountSettings, final InterledgerFulfillPacket fulfillPacket
  ) {
    Objects.requireNonNull(accountSettings);
    Objects.requireNonNull(fulfillPacket);

    // Labels: RESULT, REJECT_CODE, ACCOUNT_ID, ASSET_CODE, ASSET_SCALE
    PrometheusCollectors.outgoingPackets.labels(
        PacketStatusResult.FULFILLED.name(),
        EMPTY_REJECT_CODE,
        accountSettings.accountId().value(),
        accountSettings.assetCode(),
        stringify(accountSettings.assetScale())
    ).inc();
  }

  @Override
  public void trackOutgoingPacketRejected(
      final AccountSettings accountSettings, final InterledgerRejectPacket rejectPacket
  ) {
    Objects.requireNonNull(accountSettings);
    Objects.requireNonNull(rejectPacket);

    // Labels: RESULT, REJECT_CODE, ACCOUNT_ID, ASSET_CODE, ASSET_SCALE
    PrometheusCollectors.outgoingPackets.labels(
        PacketStatusResult.REJECTED.name(),
        rejectPacket.getCode().getCode(),
        accountSettings.accountId().value(),
        accountSettings.assetCode(),
        stringify(accountSettings.assetScale())
    ).inc();
  }

  @Override
  public void trackOutgoingPacketFailed(final AccountSettings accountSettings) {
    Objects.requireNonNull(accountSettings);

    // Labels: RESULT, REJECT_CODE, ACCOUNT_ID, ASSET_CODE, ASSET_SCALE
    PrometheusCollectors.outgoingPackets.labels(
        PacketStatusResult.FAILED.name(),
        EMPTY_REJECT_CODE,
        accountSettings.accountId().value(),
        accountSettings.assetCode(),
        stringify(accountSettings.assetScale())
    ).inc();
  }

  @Override
  public void trackNumRateLimitedPackets(final AccountSettings accountSettings) {
    Objects.requireNonNull(accountSettings);

    // Labels: RESULT, REJECT_CODE, ACCOUNT_ID, ASSET_CODE, ASSET_SCALE
    PrometheusCollectors.rateLimitedPackets.labels(
        accountSettings.accountId().value(),
        accountSettings.assetCode(),
        stringify(accountSettings.assetScale())
    ).inc();
  }

  @Override
  public void trackIncomingSettlementSucceeded(IncomingSettlementSucceededEvent event) {
    Objects.requireNonNull(event);
    event.accountSettings().ifPresent(accountSettings ->
        PrometheusCollectors.incomingSettlements.labels(
            SETTLEMENT_SUCCEEDED,
            accountSettings.accountId().value(), // accountId
            accountSettings.assetCode(), // assetCode
            accountSettings.assetScale() + "" // assetScale
        ).inc()
    );
  }

  @Override
  public void trackIncomingSettlementFailed(IncomingSettlementFailedEvent event) {
    Objects.requireNonNull(event);
    PrometheusCollectors.incomingSettlements.labels(
        SETTLEMENT_FAILED,
        event.requestedAccountId().value(), // accountId
        event.accountSettings().map(AccountSettings::assetCode).orElse(null), // assetCode
        event.accountSettings().map(as -> as.assetScale() + "").orElse(null) // assetScale
    ).inc();
  }

  @Override
  public void trackOutgoingSettlementInitiationSucceeded(final OutgoingSettlementInitiationSucceededEvent event) {
    Objects.requireNonNull(event);
    event.accountSettings().ifPresent(accountSettings ->
        PrometheusCollectors.outgoingSettlements.labels(
            SETTLEMENT_SUCCEEDED,
            accountSettings.accountId().value(), // accountId
            accountSettings.assetCode(), // assetCode
            accountSettings.assetScale() + "" // assetScale
        ).inc()
    );
  }

  @Override
  public void trackOutgoingSettlementInitiationFailed(OutgoingSettlementInitiationFailedEvent event) {
    Objects.requireNonNull(event);
    event.accountSettings().ifPresent(accountSettings ->
        PrometheusCollectors.outgoingSettlements.labels(
            SETTLEMENT_FAILED,
            accountSettings.accountId().value(), // accountId
            accountSettings.assetCode(), // assetCode
            accountSettings.assetScale() + "" // assetScale
        ).inc()
    );
  }
}
