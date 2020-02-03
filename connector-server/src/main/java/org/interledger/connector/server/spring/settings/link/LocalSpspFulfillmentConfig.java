package org.interledger.connector.server.spring.settings.link;

import static org.interledger.connector.core.ConfigConstants.ENABLED_FEATURES;
import static org.interledger.connector.core.ConfigConstants.LOCAL_SPSP_FULFILLMENT_ENABLED;
import static org.interledger.connector.core.ConfigConstants.TRUE;

import org.interledger.codecs.stream.StreamCodecContextFactory;
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

import javax.annotation.PostConstruct;

@Configuration
@ConditionalOnProperty(prefix = ENABLED_FEATURES, name = LOCAL_SPSP_FULFILLMENT_ENABLED, havingValue = TRUE)
public class LocalSpspFulfillmentConfig {

  public static final String STREAM = "STREAM";

  @Autowired
  private LinkFactoryProvider linkFactoryProvider;

  @Value("${interledger.connector.spsp.serverSecret}")
  private String spspServerSecretB64;

  @Autowired
  private StatelessSpspReceiverLinkFactory statelessSpspReceiverLinkFactory;

  @Bean
  protected StatelessSpspReceiverLinkFactory statelessSpspReceiverLinkFactory(
    PacketRejector packetRejector, StatelessStreamReceiver statelessStreamReceiver
  ) {
    return new StatelessSpspReceiverLinkFactory(packetRejector, statelessStreamReceiver);
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
  @Qualifier(STREAM)
  protected CodecContext streamCodecContext() {
    return StreamCodecContextFactory.oer();
  }

  @Bean
  protected StreamEncryptionService streamEncryptionService() {
    return new JavaxStreamEncryptionService();
  }

  @Bean
  protected StreamConnectionGenerator streamConnectionGenerator() {
    return new SpspStreamConnectionGenerator();
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

  @PostConstruct
  public void init() {
    linkFactoryProvider.registerLinkFactory(StatelessSpspReceiverLink.LINK_TYPE, statelessSpspReceiverLinkFactory);
  }

}
