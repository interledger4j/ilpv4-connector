package com.sappenin.interledger.ilpv4.connector.server.spring.settings.blast;

import com.sappenin.interledger.ilpv4.connector.accounts.BlastAccountIdResolver;
import com.sappenin.interledger.ilpv4.connector.accounts.DefaultAccountIdResolver;
import org.interledger.connector.link.LinkFactoryProvider;
import org.interledger.connector.link.blast.BlastLink;
import org.interledger.connector.link.blast.BlastLinkFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;

import static com.sappenin.interledger.ilpv4.connector.server.spring.settings.properties.ConnectorProperties.BLAST_ENABLED;

/**
 * <p>Configures ILP-over-HTTP (i.e., BLAST), which provides a single Link-layer mechanism for this Connector's
 * peers.</p>
 *
 * <p>This type of Connector supports a single HTTP/2 Server endpoint in addition to multiple, statically configured,
 * BLAST client links.</p>
 */
@Configuration
@ConditionalOnProperty(BLAST_ENABLED)
@Import({SpringConnectorWebMvc.class})
public class BlastConfig {

  @Autowired
  LinkFactoryProvider linkFactoryProvider;

  @Autowired
  @Qualifier("blast")
  RestTemplate blastRestTemplate;

  @Bean
  @Qualifier("blast")
  RestTemplate restTemplate() {
    // TODO: Configure this properly...
    return new RestTemplate();
  }

  @Bean
  BlastAccountIdResolver blastAccountIdResolver() {
    return new DefaultAccountIdResolver();
  }

  @PostConstruct
  public void startup() {
    linkFactoryProvider.registerLinkFactory(BlastLink.LINK_TYPE, new BlastLinkFactory(blastRestTemplate));
  }
}
