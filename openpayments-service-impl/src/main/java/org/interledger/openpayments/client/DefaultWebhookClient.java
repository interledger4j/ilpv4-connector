package org.interledger.openpayments.client;

import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.interledger.openpayments.Mandate;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.interleger.openpayments.client.WebhookClient;
import org.slf4j.Logger;
import org.springframework.http.HttpHeaders;

public class DefaultWebhookClient implements WebhookClient {

  private static final Logger LOGGER = getLogger(DefaultWebhookClient.class);

  private final OkHttpClient okHttpClient;
  private final ObjectMapper objectMapper;


  public DefaultWebhookClient(OkHttpClient okHttpClient, ObjectMapper objectMapper) {
    this.okHttpClient = okHttpClient;
    this.objectMapper = objectMapper;
  }

  @Override
  public void sendMandateStatusChange(Mandate mandate) {
      mandate.webhookUrl().ifPresent(webhookUrl -> {
        try {
          Request okHttpRequest = new Request.Builder()
            .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .header(HttpHeaders.ACCEPT, APPLICATION_JSON_VALUE)
            .url(webhookUrl)
            .post(RequestBody.create(
              objectMapper.writeValueAsString(mandate),
              MediaType.parse(APPLICATION_JSON_VALUE)
            ))
            .build();
          LOGGER.info("sending webhook callback for mandate {}", mandate);
          okHttpClient.newCall(okHttpRequest).execute().close();
        } catch (Exception e) {
          LOGGER.error("error while sending webhook callback", e);
        }
      });
  }
}
