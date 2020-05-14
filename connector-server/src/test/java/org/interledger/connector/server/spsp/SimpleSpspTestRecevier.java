package org.interledger.connector.server.spsp;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.spsp.StreamConnectionDetails;
import org.interledger.stream.Denomination;
import org.interledger.stream.receiver.StatelessStreamReceiver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import okhttp3.HttpUrl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.Supplier;

/**
 * Class for simulating a standalone SPSP receiver using wiremock
 */
public class SimpleSpspTestRecevier {

  public static final String TEST_RECEIVER_URI = "test-receiver";
  private static final String SPSP_TRANSFORMER = "spsp transformer";

  public static HttpUrl stubReceiver(WireMockRule rule) {
    rule.stubFor(post(anyUrl())
      .willReturn(aResponse()
        .withStatus(200)
        .withTransformers(SPSP_TRANSFORMER)
      ));
    rule.stubFor(get(anyUrl())
      .willReturn(aResponse()
        .withStatus(200)
        .withTransformers(SPSP_TRANSFORMER)
      ));

    return HttpUrl.parse(rule.baseUrl() + "/" + TEST_RECEIVER_URI);
  }

  public static ResponseDefinitionTransformer transformer(
    Supplier<StatelessStreamReceiver> statelessStreamReceiver,
    Supplier<CodecContext> codecContext,
    Supplier<ObjectMapper> objectMapper,
    InterledgerAddress receiverAddress,
    Denomination denomination) {
    return new ResponseDefinitionTransformer() {

      @Override
      public boolean applyGlobally() {
        return false;
      }

      @Override
      public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition,
                                          FileSource files, Parameters parameters) {

        if (request.getUrl().contains(TEST_RECEIVER_URI)) {
          return fulfillPacket(request, responseDefinition);
        }
        return spspSetup(responseDefinition);
      }

      private ResponseDefinition spspSetup(ResponseDefinition responseDefinition) {
        try {
          StreamConnectionDetails connectionDetails = statelessStreamReceiver.get().setupStream(receiverAddress);
          return new ResponseDefinitionBuilder().like(responseDefinition)
            .withBody(objectMapper.get().writeValueAsString(connectionDetails))
            .build();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      private ResponseDefinition fulfillPacket(Request request, ResponseDefinition responseDefinition) {
        try {
          InterledgerPreparePacket preparePacket =
            codecContext.get().read(InterledgerPreparePacket.class, new ByteArrayInputStream(request.getBody()));

          InterledgerResponsePacket responsePacket =
            statelessStreamReceiver.get().receiveMoney(preparePacket, receiverAddress, denomination);

          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          codecContext.get().write(responsePacket, baos);

          return new ResponseDefinitionBuilder().like(responseDefinition)
            .withBody(baos.toByteArray())
            .build();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public String getName() {
        return "spsp transformer";
      }

    };
  }


}
