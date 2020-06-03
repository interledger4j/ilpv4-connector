package org.interledger.connector.xumm.client;

import org.interledger.connector.xumm.model.payload.Payload;
import org.interledger.connector.xumm.model.payload.PayloadRequest;
import org.interledger.connector.xumm.model.payload.PayloadRequestResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Feign;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.optionals.OptionalDecoder;

import java.util.Objects;

public interface XummClient {

  String ACCEPT = "Accept:";
  String CONTENT_TYPE = "Content-Type:";
  String APPLICATION_JSON = "application/json";

  static XummClient construct(String xummApiKey, String xummApiSecret) {
    Objects.requireNonNull(xummApiKey);
    Objects.requireNonNull(xummApiSecret);

    ObjectMapper objectMapper = new ObjectMapper();
    return Feign.builder()
      .encoder(new JacksonEncoder(objectMapper))
      .decode404()
      .decoder(new OptionalDecoder(new JacksonDecoder(objectMapper)))
      .requestInterceptor(template -> template.header("x-api-key", xummApiKey)
        .header("x-api-secret", xummApiSecret))
      .target(XummClient.class, "https://xumm.app");
  }

  @RequestLine("POST /api/v1/platform/payload")
  @Headers( {
    ACCEPT + APPLICATION_JSON,
    CONTENT_TYPE + APPLICATION_JSON
  })
  PayloadRequestResponse createPayload(PayloadRequest payloadRequest);

  @RequestLine("GET /api/v1/platform/payload/{payloadUuid}")
  @Headers( {
    ACCEPT + APPLICATION_JSON
  })
  Payload getPayload(@Param("payloadUuid") String payloadUuid);


}
