package org.interledger.connector.metrics;

import io.prometheus.client.Counter;

/**
 * Defines various Prometheus counters, gauges, and other objects to track statistics about the Connector.
 */
public interface PrometheusCollectors {

  String RESULT = "result"; // One of `fulfilled`, `rejected`, `failed`
  String REJECT_CODE = "rejectCode"; // e.g., `F02`
  String ACCOUNT_ID = "accountId";
  String ASSET_CODE = "assetCode";
  String ASSET_SCALE = "assetScale";

  Counter incomingPackets = constructPacketCounter()
      .name("ilp_connector_incoming_ilp_packets")
      .help("Total number of incoming ILP packets.")
      .register();

  Counter outgoingPackets = constructPacketCounter()
      .name("ilp_connector_outgoing_ilp_packets")
      .help("Total number of outgoing ILP packets.")
      .register();

  Counter rateLimitedPackets = Counter.build()
      .name("ilp_connector_rate_limited_ilp_packets")
      .help("Total of rate limited ILP packets")
      .labelNames(ACCOUNT_ID, ASSET_CODE, ASSET_SCALE)
      .register();

  Counter incomingSettlements = Counter.build()
      .name("ilp_connector_incoming_settlements")
      .help("Total number of incoming settlements")
      .labelNames(RESULT, ACCOUNT_ID, ASSET_CODE, ASSET_SCALE)
      .register();

  Counter outgoingSettlements = Counter.build()
      .name("ilp_connector_outgoing_settlements")
      .help("Total number of outgoing settlements")
      .labelNames(RESULT, ACCOUNT_ID, ASSET_CODE, ASSET_SCALE)
      .register();

  static Counter.Builder constructPacketCounter() {
    return Counter.build().labelNames(RESULT, REJECT_CODE, ACCOUNT_ID, ASSET_CODE, ASSET_SCALE);
  }
}
