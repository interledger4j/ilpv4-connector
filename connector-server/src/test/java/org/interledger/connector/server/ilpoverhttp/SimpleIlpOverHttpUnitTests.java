package org.interledger.connector.server.ilpoverhttp;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.interledger.connector.jackson.ObjectMapperFactory;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.auth.BearerTokenSupplier;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import java.io.IOException;
import java.util.function.Supplier;

public class SimpleIlpOverHttpUnitTests {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private OkHttpClient okHttpClient;

  @Mock
  private CodecContext ilpCodecContext;

  @Mock
  private BearerTokenSupplier bearerTokenSupplier;

  @Mock
  private Supplier<InterledgerAddress> operatorAddressSupplier;

  private ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapperForProblemsJson();

  private IlpOverHttpLink ilpOverHttpLink;

  private HttpUrl outgoingUrl = HttpUrl.parse("http://example.com");

  @Before
  public void setUp() {
    initMocks(this);
    this.ilpOverHttpLink = new IlpOverHttpLink(
      operatorAddressSupplier,
      outgoingUrl,
      okHttpClient,
      objectMapper,
      ilpCodecContext,
      bearerTokenSupplier
    );
  }

  /**
   * Simple unit test to make sure https://github.com/interledger4j/ilpv4-connector/issues/524 is fixed.
   *
   * Will force an {@link IlpOverHttpLink} to call parseThrowableProblem with a 500 error, and test to see
   * if the result of parseThrowableProblem is returned in the message of the {@link InterledgerRejectPacket}.
   *
   * This would indicate that parseThrowableProblem did not cause an exception and did indeed parse the throwable problem.
   */
  @Test
  public void testParseThrowableProblem() throws IOException {

    Request mockRequest = new Request.Builder()
      .url("https://some-url.com")
      .build();

    // Want to force a T00_INTERNAL_ERROR, otherwise there is no way to test if parseThrowableProblem returned Optional.empty().
    // If we force a F00_BAD_REQUEST, parseThrowableProblem will log an error, but the reject packet will still just use a customErrorMessage
    // or the raw response body.
    String errorBody = "{\"title\":\"Unauthorized\",\"status\":401,\"detail\":\"Authentication failed for principal: null\"}";
    Response badResponseResponse = new Response.Builder()
      .code(500)
      .protocol(Protocol.HTTP_1_1)
      .message("")
      .body(ResponseBody.create(errorBody, MediaType.parse("application/json")))
      .request(mockRequest)
      .build();

    InterledgerAddress interledgerAddress = mock(InterledgerAddress.class);
    when(operatorAddressSupplier.get()).thenReturn(interledgerAddress);
    when(interledgerAddress.getValue()).thenReturn("foo.bar");

    Call call = mock(Call.class);
    when(okHttpClient.newCall(any())).thenReturn(call);
    when(call.execute()).thenReturn(badResponseResponse);

    InterledgerPreparePacket preparePacket = mock(InterledgerPreparePacket.class);
    InterledgerResponsePacket responsePacket = ilpOverHttpLink.sendPacket(preparePacket);

    assertThat(responsePacket).isInstanceOf(InterledgerRejectPacket.class);
    assertThat(((InterledgerRejectPacket) responsePacket).getCode()).isEqualTo(InterledgerErrorCode.T00_INTERNAL_ERROR);
    assertThat(((InterledgerRejectPacket) responsePacket).getMessage()).isEqualTo("Unauthorized");
  }

}
