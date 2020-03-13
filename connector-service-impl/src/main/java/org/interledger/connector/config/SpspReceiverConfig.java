package org.interledger.connector.config;

import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.link.LinkFactoryProvider;
import org.interledger.link.PacketRejector;
import org.interledger.link.spsp.StatelessSpspReceiverLink;
import org.interledger.link.spsp.StatelessSpspReceiverLinkFactory;
import org.interledger.stream.crypto.JavaxStreamEncryptionService;
import org.interledger.stream.crypto.Random;
import org.interledger.stream.crypto.StreamEncryptionService;
import org.interledger.stream.receiver.ServerSecretSupplier;
import org.interledger.stream.receiver.SpspStreamConnectionGenerator;
import org.interledger.stream.receiver.StatelessStreamReceiver;
import org.interledger.stream.receiver.StreamConnectionGenerator;

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

  @Value("${interledger.connector.spsp.serverSecret}")
  private String spspServerSecretB64;

  @Autowired
  private LinkFactoryProvider linkFactoryProvider;

  @Autowired
  private PacketRejector packetRejector;

  @Autowired
  @Lazy
  private StatelessStreamReceiver statelessStreamReceiver;

  @PostConstruct
  public void init() {
    linkFactoryProvider.registerLinkFactory(
      StatelessSpspReceiverLink.LINK_TYPE,
      statelessSpspReceiverLinkFactory(packetRejector, statelessStreamReceiver)
    );
  }

  @Bean
  protected ServerSecretSupplier serverSecretSupplier() {
    final byte[] serverSecret;
    if (spspServerSecretB64 != null) {
      serverSecret = Base64.getDecoder().decode(spspServerSecretB64);
    } else {
      // if `interledger.connector.spsp.serverSecret` is not specified, this value will be regenerated on every server
      // restart.
      serverSecret = Random.randBytes(32);
    }
    return () -> serverSecret;
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
  StatelessStreamReceiver statelessStreamReceiver(
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
  protected StatelessSpspReceiverLinkFactory statelessSpspReceiverLinkFactory(
    final PacketRejector packetRejector, final StatelessStreamReceiver statelessStreamReceiver
  ) {
    return new StatelessSpspReceiverLinkFactory(packetRejector, statelessStreamReceiver);
  }

}
