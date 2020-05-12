package org.interledger.connector.config;

import static org.interledger.connector.core.ConfigConstants.SPSP__SERVER_SECRET;

import org.interledger.connector.payments.FulfillmentGeneratedEventAggregator;
import org.interledger.connector.stream.TrackingStreamReceiver;
import org.interledger.connector.stream.TrackingStreamReceiverLink;
import org.interledger.connector.stream.TrackingStreamReceiverLinkFactory;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.link.LinkFactoryProvider;
import org.interledger.link.PacketRejector;
import org.interledger.stream.crypto.JavaxStreamEncryptionService;
import org.interledger.stream.crypto.Random;
import org.interledger.stream.crypto.StreamEncryptionService;
import org.interledger.stream.receiver.ServerSecretSupplier;
import org.interledger.stream.receiver.SpspStreamConnectionGenerator;
import org.interledger.stream.receiver.StatelessStreamReceiver;
import org.interledger.stream.receiver.StreamConnectionGenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.Base64;
import javax.annotation.PostConstruct;

/**
 * This configuration can apply to either a packet-forwarding or wallet-mode connector in two cases: either the
 * Connector is running in packet-forwarding mode, but local SPSP termination is enabled; or, the Connector is running
 * in wallet-mode, but SPSP is enabled/disabled.
 */
@Configuration
@Conditional(SpspReceiverEnabledCondition.class)
public class SpspReceiverConfig {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Value("${" + SPSP__SERVER_SECRET + ":}")
  private String spspServerSecretB64;

  @Autowired
  private LinkFactoryProvider linkFactoryProvider;

  @Autowired
  @Lazy
  private TrackingStreamReceiverLinkFactory trackingStreamReceiverLinkFactory;

  @PostConstruct
  public void init() {
    linkFactoryProvider.registerLinkFactory(TrackingStreamReceiverLink.LINK_TYPE, trackingStreamReceiverLinkFactory);
  }

  @Bean
  protected ServerSecretSupplier serverSecretSupplier() {
    if (spspServerSecretB64 != null && !spspServerSecretB64.isEmpty()) {
      try {
        final byte[] serverSecret = Base64.getDecoder().decode(spspServerSecretB64);
        return () -> serverSecret;
      } catch (Exception e) {
        logger.warn(String.format(
          "Unable to properly Base64 decode property `%s` with value=`%s`. Using an ephemeral value instead.",
          SPSP__SERVER_SECRET, spspServerSecretB64
        ));
        // If `interledger.connector.spsp.serverSecret` is invalid, an ephemeral version will be used.
        // This value will be regenerated on every server restart.
        final byte[] serverSecret = Random.randBytes(32);
        return () -> serverSecret;
      }
    } else {
      // If `interledger.connector.spsp.serverSecret` is not specified, an ephemeral version will be used.
      // This value will be regenerated on every server restart.
      final byte[] serverSecret = Random.randBytes(32);
      return () -> serverSecret;
    }

  }

  @Bean
  protected StreamConnectionGenerator streamConnectionGenerator() {
    return new SpspStreamConnectionGenerator();
  }

  @Bean
  protected StreamEncryptionService streamEncryptionService() {
    return new JavaxStreamEncryptionService();
  }

  @Bean
  protected StatelessStreamReceiver statelessStreamReceiver(
    final ServerSecretSupplier serverSecretSupplier,
    final StreamConnectionGenerator streamConnectionGenerator,
    final StreamEncryptionService streamEncryptionService,
    final CodecContext streamCodecContext
  ) {
    return new StatelessStreamReceiver(
      serverSecretSupplier, streamConnectionGenerator, streamEncryptionService, streamCodecContext
    );
  }

  @Bean
  protected TrackingStreamReceiverLinkFactory trackingStreamReceiverLinkFactory(
    final ServerSecretSupplier serverSecretSupplier,
    final StreamConnectionGenerator streamConnectionGenerator,
    final StreamEncryptionService streamEncryptionService,
    final CodecContext streamCodecContext,
    final PacketRejector packetRejector,
    final FulfillmentGeneratedEventAggregator fulfillmentGeneratedEventAggregator) {

    return new TrackingStreamReceiverLinkFactory(packetRejector,
      (linkSettings) ->  new TrackingStreamReceiver(
        serverSecretSupplier,
        streamConnectionGenerator,
        streamEncryptionService, streamCodecContext,
        linkSettings.accountId(),
        fulfillmentGeneratedEventAggregator
      ));
  }

}
