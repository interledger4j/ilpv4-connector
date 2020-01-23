package org.interledger.connector.server.spring.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.connector.server.spring.controllers.IlpHttpController.APPLICATION_ILP_OCTET_STREAM_VALUE;
import static org.interledger.connector.server.spring.controllers.PathConstants.SLASH;
import static org.interledger.connector.server.spring.controllers.PathConstants.SLASH_ACCOUNTS;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerConstants;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.encoding.asn.framework.CodecContext;

import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = IlpHttpControllerTest.class)
public class IlpHttpControllerTest extends AbstractControllerTest {

  @Autowired
  private MockMvc mvc;

  @Autowired
  private CodecContext ilpCodecContext;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testApplicationOctetStream() throws Exception {
    postIlpPrepare(testOctetStreamHeaders(), HttpStatus.OK);
  }

  @Test
  public void testApplicationIlpOctetStream() throws Exception {
    postIlpPrepare(contentTypeHeader(APPLICATION_ILP_OCTET_STREAM_VALUE), HttpStatus.OK);
  }

  @Test
  public void testContentTypeCovfefe() throws Exception {
    postIlpPrepare(contentTypeHeader("application/covfefe"), HttpStatus.UNSUPPORTED_MEDIA_TYPE);
  }

  private void postIlpPrepare(HttpHeaders httpHeaders, HttpStatus expectedStatus) throws Exception {
    String accountId = "foo";
    InterledgerPreparePacket preparePacket = InterledgerPreparePacket.builder()
      .executionCondition(InterledgerConstants.ALL_ZEROS_CONDITION)
      .amount(UnsignedLong.ONE)
      .destination(InterledgerAddress.of("test.foo"))
      .expiresAt(new Date().toInstant())
      .build();

    MvcResult result = this.mvc
      .perform(post(SLASH_ACCOUNTS + SLASH + accountId + "/ilp")
        .headers(httpHeaders)
        .content(serialize(preparePacket))
        .with(httpBasic("admin", "password")).with(csrf())
      ).andReturn();
    assertThat(result.getResponse().getStatus()).isEqualTo(expectedStatus.value());
  }

  private HttpHeaders contentTypeHeader(String contentType) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.valueOf(contentType));
    return headers;
  }

  private byte[] serialize(InterledgerPreparePacket preparePacket) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ilpCodecContext.write(preparePacket, baos);
    return baos.toByteArray();
  }

}
