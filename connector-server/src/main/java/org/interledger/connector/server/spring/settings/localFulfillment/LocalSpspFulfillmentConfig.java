package org.interledger.connector.server.spring.settings.localFulfillment;

import static org.interledger.connector.core.ConfigConstants.ENABLED_FEATURES;
import static org.interledger.connector.core.ConfigConstants.LOCAL_SPSP_FULFILLMENT_ENABLED;
import static org.interledger.connector.core.ConfigConstants.TRUE;

import org.interledger.codecs.stream.StreamCodecContextFactory;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.core.InterledgerAddressPrefix;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Base64;
import java.util.function.Supplier;

import javax.annotation.PostConstruct;

@Configuration
@ConditionalOnProperty(prefix = ENABLED_FEATURES, name = LOCAL_SPSP_FULFILLMENT_ENABLED, havingValue = TRUE)
public class LocalSpspFulfillmentConfig {

  public static final String STREAM = "STREAM";

  @Autowired
  private LinkFactoryProvider linkFactoryProvider;

  @Value("${interledger.connector.spsp.server-secret}")
  private String spspServerSecretB64;

  @Value("${interledger.connector.spsp.address-prefix-segment}")
  private String spspAddressPrefixSegment;

  @Autowired
  private StatelessSpspReceiverLinkFactory statelessSpspReceiverLinkFactory;

  @Bean
  StatelessSpspReceiverLinkFactory statelessSpspReceiverLinkFactory(
    PacketRejector packetRejector, StatelessStreamReceiver statelessStreamReceiver
  ) {
    return new StatelessSpspReceiverLinkFactory(packetRejector, statelessStreamReceiver);
  }

  @Bean
  InterledgerAddressPrefix spspAddressPrefix(Supplier<ConnectorSettings> connectorSettingsSupplier) {
    return InterledgerAddressPrefix.of(connectorSettingsSupplier.get().operatorAddress().getValue())
      .with(spspAddressPrefixSegment);
  }

  @Bean
  ServerSecretSupplier serverSecretSupplier() {
    final byte[] serverSecret;
    if (spspServerSecretB64 != null) {
      serverSecret = Base64.getDecoder().decode(spspServerSecretB64);
    } else {
      // if `interledger.connector.spsp.server-secret` is not specified, this value will be regenerated on every server
      // restart.
      serverSecret = Random.randBytes(32);
    }
    return () -> serverSecret;
  }

  @Bean
  @Qualifier(STREAM)
  CodecContext streamCodecContext() {
    return StreamCodecContextFactory.oer();
  }

  @Bean
  StreamEncryptionService streamEncryptionService() {
    return new JavaxStreamEncryptionService();
  }

  @Bean
  StreamConnectionGenerator streamConnectionGenerator() {
    return new SpspStreamConnectionGenerator();
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

  @PostConstruct
  public void init() {
    linkFactoryProvider.registerLinkFactory(StatelessSpspReceiverLink.LINK_TYPE, statelessSpspReceiverLinkFactory);
  }

}
