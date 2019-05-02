package com.sappenin.interledger.ilpv4.connector.server.spring.settings.blast;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.sappenin.interledger.ilpv4.connector.accounts.BlastAccountIdResolver;
import com.sappenin.interledger.ilpv4.connector.accounts.DefaultAccountIdResolver;
import com.sappenin.interledger.ilpv4.connector.server.spring.controllers.converters.OerPreparePacketHttpMessageConverter;
import org.interledger.connector.link.LinkFactoryProvider;
import org.interledger.connector.link.blast.BlastLink;
import org.interledger.connector.link.blast.BlastLinkFactory;
import org.interledger.connector.link.events.LinkEventEmitter;
import org.interledger.crypto.Decryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;

import static com.sappenin.interledger.ilpv4.connector.server.spring.settings.properties.ConnectorProperties.BLAST_ENABLED;
import static com.sappenin.interledger.ilpv4.connector.server.spring.settings.properties.ConnectorProperties.ENABLED_PROTOCOLS;

/**
 * <p>Configures ILP-over-HTTP (i.e., BLAST), which provides a single Link-layer mechanism for this Connector's
 * peers.</p>
 *
 * <p>This type of Connector supports a single HTTP/2 Server endpoint in addition to multiple, statically configured,
 * BLAST client links.</p>
 */
@Configuration
@ConditionalOnProperty(prefix = ENABLED_PROTOCOLS, name = BLAST_ENABLED, havingValue = "true")
public class BlastConfig {

  public static final String BLAST = "blast";

  @Autowired
  LinkEventEmitter linkEventEmitter;

  @Autowired
  LinkFactoryProvider linkFactoryProvider;

  @Autowired
  @Qualifier(BLAST)
  RestTemplate blastRestTemplate;

  @Autowired
  Decryptor decryptor;

  @Bean
  @Qualifier(BLAST)
  RestTemplate restTemplate(
    ObjectMapper objectMapper, OerPreparePacketHttpMessageConverter oerPreparePacketHttpMessageConverter
  ) {
    final MappingJackson2HttpMessageConverter httpMessageConverter =
      new MappingJackson2HttpMessageConverter(objectMapper);

    return new RestTemplate(
      Lists.newArrayList(oerPreparePacketHttpMessageConverter, httpMessageConverter)
    );
  }

  @Bean
  BlastAccountIdResolver blastAccountIdResolver() {
    return new DefaultAccountIdResolver();
  }

  @PostConstruct
  public void startup() {
    linkFactoryProvider.registerLinkFactory(
      BlastLink.LINK_TYPE, new BlastLinkFactory(linkEventEmitter, blastRestTemplate, decryptor)
    );
  }
}
