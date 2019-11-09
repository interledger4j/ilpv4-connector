package org.interledger.connector.server.spring.settings.metrics;

import org.interledger.connector.events.IncomingSettlementFailedEvent;
import org.interledger.connector.events.IncomingSettlementSucceededEvent;
import org.interledger.connector.events.OutgoingSettlementInitiationFailedEvent;
import org.interledger.connector.events.OutgoingSettlementInitiationSucceededEvent;
import org.interledger.connector.metrics.MetricsService;
import org.interledger.connector.metrics.PrometheusMetricsService;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import io.prometheus.client.exporter.MetricsServlet;
import io.prometheus.client.hotspot.DefaultExports;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;

import javax.annotation.PostConstruct;

/**
 * Configures any metrics collection for this Connector.
 *
 * <p>NOTE: Only settlement activities are tracked via events using {@link EventBus} (packet-related actions are not
 * tracked in this way), for two reasons. First, the event model is superior in general to using the MetricsService
 * directly in classes because this de-couples services (like SettlementService) from anything else we might be doing
 * passively, like tracking metrics. That said, the packet actions like fulfill, reject, failure are _not_ tracked using
 * events because it would add roughly 4 new object instantiations on every packet (which might be a performance hit) so
 * for now we use the MetricsService directly for to tell the metrics service about `balance` and `packet` metrics.</p>
 */
@Configuration
@SuppressWarnings( {"UnstableApiUsage", "unused"})
public class MetricsConfiguration {

  // This would typically be `/metrics`, but Spring Boot actuator uses this endpoint, and we use prometheus.
  public static final String METRICS_ENDPOINT_URL_PATH = "/prometheus";

  @Autowired
  private EventBus eventBus;

  @Autowired
  private MetricsService metricsService;

  @PostConstruct
  protected void initMetrics() {
    eventBus.register(this);
  }

  @Bean
  public MetricsService statisticsService() {
    return new PrometheusMetricsService();
  }

  @Bean
  public ServletRegistrationBean servletRegistrationBean() {
    DefaultExports.initialize();
    return new ServletRegistrationBean(new MetricsServlet(), METRICS_ENDPOINT_URL_PATH);
  }

  //////////////////////////
  // Event Bus Subscriptions
  //////////////////////////

  // NOTE: See Javadoc note in this class for why we don't use the EventBus for packet pipeline tracking.

  /**
   * Updates Prometheus whenever an incoming settlement has been processed.
   *
   * @param event A {@link IncomingSettlementSucceededEvent} event.
   */
  @Subscribe
  public void trackIncomingSettlementSucceeded(IncomingSettlementSucceededEvent event) {
    Objects.requireNonNull(event);
    metricsService.trackIncomingSettlementSucceeded(event);
  }

  /**
   * Updates Prometheus whenever an incoming settlement request has failed.
   *
   * @param event A {@link IncomingSettlementFailedEvent} event.
   */
  @Subscribe
  public void trackIncomingSettlementSucceeded(IncomingSettlementFailedEvent event) {
    Objects.requireNonNull(event);
    metricsService.trackIncomingSettlementFailed(event);
  }

  /**
   * Updates Prometheus whenever an outgoing settlement initiation has been successfully sent to the settlement engine.
   *
   * @param event A {@link OutgoingSettlementInitiationSucceededEvent} event.
   */
  @Subscribe
  public void trackOutgoingSettlementInitiationsSucceeded(final OutgoingSettlementInitiationSucceededEvent event) {
    Objects.requireNonNull(event);
    metricsService.trackOutgoingSettlementInitiationSucceeded(event);
  }

  /**
   * Updates Prometheus whenever an outgoing settlement initiation has been successfully sent to the settlement engine.
   *
   * @param event A {@link OutgoingSettlementInitiationSucceededEvent} event.
   */
  @Subscribe
  public void trackOutgoingSettlementInitiationsFailed(final OutgoingSettlementInitiationFailedEvent event) {
    Objects.requireNonNull(event);
    metricsService.trackOutgoingSettlementInitiationFailed(event);
  }

}
