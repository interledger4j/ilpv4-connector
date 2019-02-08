package com.sappenin.interledger.ilpv4.connector.server.spring.settings.blast;

import com.sappenin.interledger.ilpv4.connector.accounts.BlastAccountIdResolver;
import com.sappenin.interledger.ilpv4.connector.accounts.DefaultAccountIdResolver;
import com.sappenin.interledger.ilpv4.connector.server.spring.settings.ConnectorProfile;
import org.interledger.lpiv2.blast.BlastPlugin;
import org.interledger.lpiv2.blast.BlastPluginFactory;
import org.interledger.plugin.lpiv2.btp2.spring.factories.PluginFactoryProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;

import static com.sappenin.interledger.ilpv4.connector.server.spring.settings.properties.ConnectorProperties.BLAST_ENABLED;

/**
 * <p>Configures BLAST for a Connector running in `connector-mode`. This type of Connector supports a single HTTP/2
 * Server endpoint in addition to multiple, statically configured, BLAST client plugins.</p>
 */
@Configuration
@Profile(ConnectorProfile.CONNECTOR_MODE)
@ConditionalOnProperty(BLAST_ENABLED)
public class BlastConnectorModeConfig {

  @Autowired
  PluginFactoryProvider pluginFactoryProvider;

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
    pluginFactoryProvider.registerPluginFactory(BlastPlugin.PLUGIN_TYPE, new BlastPluginFactory(blastRestTemplate));
  }
}
