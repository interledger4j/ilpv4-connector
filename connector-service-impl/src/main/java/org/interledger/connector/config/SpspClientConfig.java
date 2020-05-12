package org.interledger.connector.config;

import org.interledger.spsp.client.SimpleSpspClient;
import org.interledger.spsp.client.SpspClient;
import org.interledger.spsp.client.SpspClientDefaults;

import okhttp3.HttpUrl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpspClientConfig {

  @Bean
  protected SpspClient spspClient() {
    return new SimpleSpspClient(SpspClientDefaults.OK_HTTP_CLIENT,
      paymentPointer -> {
      // default to HTTPS if no port specified, or port 443 specified
        if (!paymentPointer.host().contains(":") || paymentPointer.host().endsWith(":443")) {
          return HttpUrl.parse("https://" + paymentPointer.host() + paymentPointer.path());
        }
        return HttpUrl.parse("http://" + paymentPointer.host() + paymentPointer.path());
      },
      SpspClientDefaults.MAPPER);
  }

}
