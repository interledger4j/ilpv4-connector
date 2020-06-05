package org.interledger.connector.payid;

import org.interledger.connector.opa.model.PayId;
import org.interledger.connector.opa.model.PaymentNetwork;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Feign;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import feign.Target;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.optionals.OptionalDecoder;

import java.net.URI;

public interface FeignPayIdClient extends PayIdClient {

  static FeignPayIdClient construct(ObjectMapper objectMapper) {
    return Feign.builder()
      .encoder(new JacksonEncoder(objectMapper))
      .decode404()
      .decoder(new OptionalDecoder(new JacksonDecoder(objectMapper)))
      .target(Target.EmptyTarget.create(FeignPayIdClient.class));
  }

  @RequestLine("GET /{account}")
  @Headers( {
    "PayID-Version: 1.0",
    "Accept: application/{paymentNetwork}-{environment}+json"
  })
  PayIdResponse getPayId(URI baseUri,
                         @Param("account") String account,
                         @Param("paymentNetwork") PaymentNetwork paymentNetwork,
                         @Param("environment") String environment);

  default PayIdResponse getPayId(PayId payId, PaymentNetwork paymentNetwork, String environment) {
    return getPayId(payId.baseUrl().uri(), payId.account(), paymentNetwork, environment);
  }

}