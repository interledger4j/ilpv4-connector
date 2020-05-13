package org.interledger.connector.server.spsp;

import org.interledger.spsp.client.SimpleSpspClient;
import org.interledger.spsp.client.SpspClient;
import org.interledger.spsp.client.SpspClientDefaults;

import okhttp3.HttpUrl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;


/**
 * Overrides the {@link SpspClient} to one that resolves using HTTP instead of HTTPS so that it can be used
 * to call a locally running spring boot test server.
 */
@Configuration
public class LocalSpspClientTestConfig {

  @Primary
  @Bean
  protected SpspClient localSpspClient() {
    return new SimpleSpspClient(SpspClientDefaults.OK_HTTP_CLIENT,
      (paymentPointer) -> HttpUrl.parse("http://" + paymentPointer.host() + paymentPointer.path()),
      SpspClientDefaults.MAPPER);
  }

}