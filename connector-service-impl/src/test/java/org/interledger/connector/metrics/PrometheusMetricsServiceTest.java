package org.interledger.connector.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.SettlementEngineAccountId;
import org.interledger.connector.core.settlement.SettlementQuantity;
import org.interledger.connector.events.IncomingSettlementFailedEvent;
import org.interledger.connector.events.IncomingSettlementSucceededEvent;
import org.interledger.connector.events.OutgoingSettlementInitiationFailedEvent;
import org.interledger.connector.events.OutgoingSettlementInitiationSucceededEvent;
import org.interledger.connector.metrics.MetricsService.PacketStatusResult;
import org.interledger.connector.settlement.SettlementServiceException;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.link.LoopbackLink;

import com.google.common.primitives.UnsignedLong;
import io.prometheus.client.Counter;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Arrays;

/**
 * Unit tests for {@link PrometheusMetricsService}.
 */
public class PrometheusMetricsServiceTest {

  private static final InterledgerAddress DESTINATION_ADDRESS = InterledgerAddress.of("example.destination");

  private static final String SETTLEMENT_SUCCEEDED = "succeeded";
  private static final String SETTLEMENT_FAILED = "failed";

  private PrometheusMetricsService metricsService;

  private static String[] counterKey(
      final PacketStatusResult packetStatusResult, final AccountSettings accountSettings
  ) {
    return Arrays.asList(
        packetStatusResult.name(),
        "",
        accountSettings.accountId().value(),
        accountSettings.assetCode(),
        accountSettings.assetScale() + ""
    ).toArray(new String[0]);
  }

  private static String[] counterKey(
      final String settlementOutcome, final AccountSettings accountSettings
  ) {
    return Arrays.asList(
        settlementOutcome,
        accountSettings.accountId().value(),
        accountSettings.assetCode(),
        accountSettings.assetScale() + ""
    ).toArray(new String[0]);
  }

  @Before
  public void setUp() {
    resetPrometheusMetrics();
    this.metricsService = new PrometheusMetricsService();
  }

  @Test
  public void trackIncomingPacketPrepared() {
    final Counter counter = PrometheusCollectors.incomingPackets;
    assertThat(sum(counter)).isEqualTo(0);
    for (int i = 0; i < 100; i++) {
      metricsService.trackIncomingPacketPrepared(accountSettings(), preparePacket());
      assertThat(sum(counter, PacketStatusResult.PREPARED.name())).isEqualTo(i + 1);
      assertThat(sum(counter, PacketStatusResult.FULFILLED.name())).isEqualTo(0);
      assertThat(sum(counter, PacketStatusResult.REJECTED.name())).isEqualTo(0);
      assertThat(sum(counter, PacketStatusResult.FAILED.name())).isEqualTo(0);
    }
  }

  @Test
  public void trackIncomingPacketFulfilled() {
    final Counter counter = PrometheusCollectors.incomingPackets;
    assertThat(sum(counter)).isEqualTo(0);
    for (int i = 0; i < 100; i++) {
      metricsService.trackIncomingPacketFulfilled(accountSettings(), fulfillPacket());
      assertThat(sum(counter, PacketStatusResult.PREPARED.name())).isEqualTo(0);
      assertThat(sum(counter, PacketStatusResult.FULFILLED.name())).isEqualTo(i + 1);
      assertThat(sum(counter, PacketStatusResult.REJECTED.name())).isEqualTo(0);
      assertThat(sum(counter, PacketStatusResult.FAILED.name())).isEqualTo(0);
    }
  }

  @Test
  public void trackIncomingPacketRejected() {
    final Counter counter = PrometheusCollectors.incomingPackets;
    assertThat(sum(counter)).isEqualTo(0);
    for (int i = 0; i < 100; i++) {
      metricsService.trackIncomingPacketRejected(accountSettings(), rejectPacket());
      assertThat(sum(counter, PacketStatusResult.PREPARED.name())).isEqualTo(0);
      assertThat(sum(counter, PacketStatusResult.FULFILLED.name())).isEqualTo(0);
      assertThat(sum(counter, PacketStatusResult.REJECTED.name())).isEqualTo(i + 1);
      assertThat(sum(counter, PacketStatusResult.FAILED.name())).isEqualTo(0);
    }
  }

  @Test
  public void trackIncomingPacketFailed() {
    final Counter counter = PrometheusCollectors.incomingPackets;
    assertThat(sum(counter)).isEqualTo(0);
    for (int i = 0; i < 100; i++) {
      metricsService.trackIncomingPacketFailed(accountSettings());
      assertThat(sum(counter, PacketStatusResult.PREPARED.name())).isEqualTo(0);
      assertThat(sum(counter, PacketStatusResult.FULFILLED.name())).isEqualTo(0);
      assertThat(sum(counter, PacketStatusResult.REJECTED.name())).isEqualTo(0);
      assertThat(sum(counter, PacketStatusResult.FAILED.name())).isEqualTo(i + 1);
    }
  }

  @Test
  public void trackOutgoingPacketPrepared() {
    final Counter counter = PrometheusCollectors.outgoingPackets;
    assertThat(sum(counter)).isEqualTo(0);
    for (int i = 0; i < 100; i++) {
      metricsService.trackOutgoingPacketPrepared(accountSettings(), preparePacket());
      assertThat(sum(counter, PacketStatusResult.PREPARED.name())).isEqualTo(i + 1);
      assertThat(sum(counter, PacketStatusResult.FULFILLED.name())).isEqualTo(0);
      assertThat(sum(counter, PacketStatusResult.REJECTED.name())).isEqualTo(0);
      assertThat(sum(counter, PacketStatusResult.FAILED.name())).isEqualTo(0);
    }
  }

  @Test
  public void trackOutgoingPacketFulfilled() {
    final Counter counter = PrometheusCollectors.outgoingPackets;
    assertThat(sum(counter)).isEqualTo(0);
    for (int i = 0; i < 100; i++) {
      metricsService.trackOutgoingPacketFulfilled(accountSettings(), fulfillPacket());
      assertThat(sum(counter, PacketStatusResult.PREPARED.name())).isEqualTo(0);
      assertThat(sum(counter, PacketStatusResult.FULFILLED.name())).isEqualTo(i + 1);
      assertThat(sum(counter, PacketStatusResult.REJECTED.name())).isEqualTo(0);
      assertThat(sum(counter, PacketStatusResult.FAILED.name())).isEqualTo(0);
    }
  }

  @Test
  public void trackOutgoingPacketRejected() {
    final Counter counter = PrometheusCollectors.outgoingPackets;
    assertThat(sum(counter)).isEqualTo(0);
    for (int i = 0; i < 100; i++) {
      metricsService.trackOutgoingPacketRejected(accountSettings(), rejectPacket());
      assertThat(sum(counter, PacketStatusResult.PREPARED.name())).isEqualTo(0);
      assertThat(sum(counter, PacketStatusResult.FULFILLED.name())).isEqualTo(0);
      assertThat(sum(counter, PacketStatusResult.REJECTED.name())).isEqualTo(i + 1);
      assertThat(sum(counter, PacketStatusResult.FAILED.name())).isEqualTo(0);
    }
  }

  @Test
  public void trackOutgoingPacketFailed() {
    final Counter counter = PrometheusCollectors.outgoingPackets;
    assertThat(sum(counter)).isEqualTo(0);
    for (int i = 0; i < 100; i++) {
      metricsService.trackOutgoingPacketFailed(accountSettings());
      assertThat(sum(counter, PacketStatusResult.PREPARED.name())).isEqualTo(0);
      assertThat(sum(counter, PacketStatusResult.FULFILLED.name())).isEqualTo(0);
      assertThat(sum(counter, PacketStatusResult.REJECTED.name())).isEqualTo(0);
      assertThat(sum(counter, PacketStatusResult.FAILED.name())).isEqualTo(i + 1);
    }
  }

  @Test
  public void trackNumRateLimitedPackets() {
    final Counter counter = PrometheusCollectors.rateLimitedPackets;
    assertThat(sum(counter)).isEqualTo(0);
    for (int i = 0; i < 100; i++) {
      metricsService.trackNumRateLimitedPackets(accountSettings());
      assertThat(sum(counter)).isEqualTo(i + 1);
    }
  }

  @Test
  public void trackIncomingSettlementSucceeded() {
    final Counter counter = PrometheusCollectors.incomingSettlements;
    assertThat(sum(counter)).isEqualTo(0);
    for (int i = 0; i < 100; i++) {
      metricsService.trackIncomingSettlementSucceeded(incomingSettlementSucceededEvent());
      assertThat(sum(counter)).isEqualTo(i + 1);
      assertThat(sum(counter, SETTLEMENT_SUCCEEDED)).isEqualTo(i + 1);
      assertThat(sum(counter, SETTLEMENT_FAILED)).isEqualTo(0);
    }

    metricsService.trackIncomingSettlementFailed(incomingSettlementFailedEvent());

    assertThat(sum(counter)).isEqualTo(101);
    assertThat(sum(counter, SETTLEMENT_SUCCEEDED)).isEqualTo(100);
    assertThat(sum(counter, SETTLEMENT_FAILED)).isEqualTo(1);
  }

  @Test
  public void trackIncomingSettlementFailed() {
    final Counter counter = PrometheusCollectors.incomingSettlements;
    assertThat(sum(counter)).isEqualTo(0);
    for (int i = 0; i < 100; i++) {
      metricsService.trackIncomingSettlementFailed(incomingSettlementFailedEvent());
      assertThat(sum(counter)).isEqualTo(i + 1);
      assertThat(sum(counter, SETTLEMENT_SUCCEEDED)).isEqualTo(0);
      assertThat(sum(counter, SETTLEMENT_FAILED)).isEqualTo(i + 1);
    }

    metricsService.trackIncomingSettlementSucceeded(incomingSettlementSucceededEvent());

    assertThat(sum(counter)).isEqualTo(101);
    assertThat(sum(counter, SETTLEMENT_SUCCEEDED)).isEqualTo(1);
    assertThat(sum(counter, SETTLEMENT_FAILED)).isEqualTo(100);
  }

  @Test
  public void trackOutgoingSettlementInitiationSucceeded() {
    final Counter counter = PrometheusCollectors.outgoingSettlements;
    assertThat(sum(counter)).isEqualTo(0);
    for (int i = 0; i < 100; i++) {
      metricsService.trackOutgoingSettlementInitiationSucceeded(outgoingSettlementInitiationSucceededEvent());
      assertThat(sum(counter)).isEqualTo(i + 1);
      assertThat(sum(counter, SETTLEMENT_SUCCEEDED)).isEqualTo(i + 1);
      assertThat(sum(counter, SETTLEMENT_FAILED)).isEqualTo(0);
    }

    metricsService.trackOutgoingSettlementInitiationFailed(outgoingSettlementInitiationFailedEvent());

    assertThat(sum(counter)).isEqualTo(101);
    assertThat(sum(counter, SETTLEMENT_SUCCEEDED)).isEqualTo(100);
    assertThat(sum(counter, SETTLEMENT_FAILED)).isEqualTo(1);
  }

  @Test
  public void trackOutgoingSettlementInitiationFailed() {
    final Counter counter = PrometheusCollectors.outgoingSettlements;
    assertThat(sum(counter)).isEqualTo(0);
    for (int i = 0; i < 100; i++) {
      metricsService.trackOutgoingSettlementInitiationFailed(outgoingSettlementInitiationFailedEvent());
      assertThat(sum(counter)).isEqualTo(i + 1);
      assertThat(sum(counter, SETTLEMENT_SUCCEEDED)).isEqualTo(0);
      assertThat(sum(counter, SETTLEMENT_FAILED)).isEqualTo(i + 1);
    }

    metricsService.trackOutgoingSettlementInitiationSucceeded(outgoingSettlementInitiationSucceededEvent());

    assertThat(sum(counter)).isEqualTo(101);
    assertThat(sum(counter, SETTLEMENT_SUCCEEDED)).isEqualTo(1);
    assertThat(sum(counter, SETTLEMENT_FAILED)).isEqualTo(100);
  }

  //////////////////
  // Private Helpers
  //////////////////

  private IncomingSettlementSucceededEvent incomingSettlementSucceededEvent() {
    return IncomingSettlementSucceededEvent.builder()
        .accountSettings(accountSettings())
        .idempotencyKey("foo")
        .settlementEngineAccountId(SettlementEngineAccountId.of("seAccountId"))
        .incomingSettlementInSettlementUnits(
            SettlementQuantity.builder().amount(BigInteger.ZERO).scale(2).build()
        )
        .processedQuantity(SettlementQuantity.builder().amount(BigInteger.TEN).scale(2).build())
        .build();
  }

  private IncomingSettlementFailedEvent incomingSettlementFailedEvent() {
    return IncomingSettlementFailedEvent.builder()
        .accountSettings(accountSettings())
        .idempotencyKey("foo")
        .settlementEngineAccountId(SettlementEngineAccountId.of("seAccountId"))
        .incomingSettlementInSettlementUnits(
            SettlementQuantity.builder().amount(BigInteger.ZERO).scale(2).build()
        )
        .settlementServiceException(
            new SettlementServiceException(AccountId.of("testAccountId"), SettlementEngineAccountId.of("seAccountId"))
        )
        .build();
  }

  private OutgoingSettlementInitiationSucceededEvent outgoingSettlementInitiationSucceededEvent() {
    return OutgoingSettlementInitiationSucceededEvent.builder()
        .accountSettings(accountSettings())
        .idempotencyKey("foo")
        .processedQuantityInClearingUnits(
            SettlementQuantity.builder().amount(BigInteger.ZERO).scale(2).build()
        )
        .settlementQuantityInClearingUnits(SettlementQuantity.builder().amount(BigInteger.TEN).scale(2).build())
        .build();
  }

  private OutgoingSettlementInitiationFailedEvent outgoingSettlementInitiationFailedEvent() {
    return OutgoingSettlementInitiationFailedEvent.builder()
        .accountSettings(accountSettings())
        .idempotencyKey("foo")
        .settlementQuantityInClearingUnits(SettlementQuantity.builder().amount(BigInteger.TEN).scale(2).build())
        .settlementServiceException(
            new SettlementServiceException(AccountId.of("testAccountId"), SettlementEngineAccountId.of("seAccountId"))
        )
        .build();
  }

  private AccountSettings accountSettings() {
    return AccountSettings.builder()
        .accountId(AccountId.of("testAccountId"))
        .accountRelationship(AccountRelationship.PEER)
        .assetScale(9)
        .assetCode("XRP")
        .linkType(LoopbackLink.LINK_TYPE)
        .build();
  }

  private InterledgerPreparePacket preparePacket() {
    return InterledgerPreparePacket.builder()
        .destination(DESTINATION_ADDRESS)
        .executionCondition(InterledgerCondition.of(new byte[32]))
        .amount(UnsignedLong.ZERO)
        .expiresAt(Instant.MAX)
        .build();
  }

  private InterledgerFulfillPacket fulfillPacket() {
    return InterledgerFulfillPacket.builder().fulfillment(InterledgerFulfillment.of(new byte[32])).build();
  }

  private InterledgerRejectPacket rejectPacket() {
    return InterledgerRejectPacket.builder()
        .code(InterledgerErrorCode.F99_APPLICATION_ERROR)
        .build();
  }

  private int sum(final Counter counter) {
    return (int) counter.collect().stream()
        .mapToDouble(foo -> foo.samples.stream()
            .map(sample -> sample.value)
            .reduce(0.0, Double::sum))
        .reduce(0.0, Double::sum);
  }

  private int sum(final Counter counter, final String label) {
    return (int) counter.collect().stream()
        .mapToDouble(foo -> foo.samples.stream()
            .filter(sample -> sample.labelValues.contains(label))
            .map(sample -> sample.value)
            .reduce(0.0, Double::sum))
        .reduce(0.0, Double::sum);
  }


  private void resetPrometheusMetrics() {
    PrometheusCollectors.incomingPackets.remove(counterKey(PacketStatusResult.PREPARED, accountSettings()));
    PrometheusCollectors.incomingPackets.remove(counterKey(PacketStatusResult.FULFILLED, accountSettings()));
    PrometheusCollectors.incomingPackets.remove(counterKey(PacketStatusResult.REJECTED, accountSettings()));
    PrometheusCollectors.incomingPackets.remove(counterKey(PacketStatusResult.FAILED, accountSettings()));

    PrometheusCollectors.outgoingPackets.remove(counterKey(PacketStatusResult.PREPARED, accountSettings()));
    PrometheusCollectors.outgoingPackets.remove(counterKey(PacketStatusResult.FULFILLED, accountSettings()));
    PrometheusCollectors.outgoingPackets.remove(counterKey(PacketStatusResult.REJECTED, accountSettings()));
    PrometheusCollectors.outgoingPackets.remove(counterKey(PacketStatusResult.FAILED, accountSettings()));

    PrometheusCollectors.incomingSettlements.remove(counterKey(SETTLEMENT_SUCCEEDED, accountSettings()));
    PrometheusCollectors.incomingSettlements.remove(counterKey(SETTLEMENT_FAILED, accountSettings()));
    PrometheusCollectors.outgoingSettlements.remove(counterKey(SETTLEMENT_SUCCEEDED, accountSettings()));
    PrometheusCollectors.outgoingSettlements.remove(counterKey(SETTLEMENT_FAILED, accountSettings()));
  }
}
