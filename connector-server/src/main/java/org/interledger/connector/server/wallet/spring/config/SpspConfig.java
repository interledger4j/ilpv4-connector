package org.interledger.connector.server.wallet.spring.config;

import static org.interledger.connector.core.ConfigConstants.ENABLED_PROTOCOLS;
import static org.interledger.connector.core.ConfigConstants.SPSP_ENABLED;
import static org.interledger.connector.core.ConfigConstants.TRUE;

import org.interledger.codecs.stream.StreamCodecContextFactory;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.stream.crypto.JavaxStreamEncryptionService;
import org.interledger.stream.crypto.StreamEncryptionService;
import org.interledger.stream.receiver.ServerSecretSupplier;
import org.interledger.stream.receiver.SpspStreamConnectionGenerator;
import org.interledger.stream.receiver.StatelessStreamReceiver;
import org.interledger.stream.receiver.StreamConnectionGenerator;
import org.interledger.stream.receiver.StreamReceiver;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = ENABLED_PROTOCOLS, name = SPSP_ENABLED, havingValue = TRUE)
public class SpspConfig {

  public static final String STREAM = "STREAM";

  @Bean
  @Qualifier(STREAM)
  public CodecContext streamCodecContext() {
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
  protected StreamReceiver streamReceiver(
    final ServerSecretSupplier serverSecretSupplier,
    final StreamConnectionGenerator streamConnectionGenerator,
    final StreamEncryptionService streamEncryptionService,
    final CodecContext streamCodecContext
  ) {
    return new StatelessStreamReceiver(
      serverSecretSupplier, streamConnectionGenerator, streamEncryptionService, streamCodecContext
    );
  }

}
