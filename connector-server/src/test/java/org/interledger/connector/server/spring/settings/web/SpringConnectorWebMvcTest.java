package org.interledger.connector.server.spring.settings.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;

import org.interledger.codecs.ilp.InterledgerCodecContextFactory;
import org.interledger.connector.jackson.ObjectMapperFactory;
import org.interledger.connector.server.spring.controllers.converters.OerPreparePacketHttpMessageConverter;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for {@linkn SpringConnectorWebMvc}.
 */
public class SpringConnectorWebMvcTest {

  private SpringConnectorWebMvc springConnectorWebMvc;

  @Before
  public void setUp() {
    springConnectorWebMvc = new SpringConnectorWebMvc();
  }

  @Test
  public void testExtendMessageConvertersWhenEmpty() {
    List<HttpMessageConverter<?>> messageConverters = Lists.newArrayList();
    springConnectorWebMvc.extendMessageConverters(messageConverters);

    assertThat(messageConverters.size()).isEqualTo(0);
  }

  @Test
  public void testExtendMessageConvertersWithoutProblemsJsonHttpConverters() {
    final List<HttpMessageConverter<?>> originalMessageConverters = configureInitialMessageConvertersWithoutProblems();

    // Copy the list.
    final List<HttpMessageConverter<?>> adjustedMessageConverters = new ArrayList<>(originalMessageConverters);
    springConnectorWebMvc.extendMessageConverters(adjustedMessageConverters);

    // First Converter
    assertThat(originalMessageConverters.size()).isEqualTo(3);
    assertThat(adjustedMessageConverters.size()).isEqualTo(3);

    assertThat(originalMessageConverters.get(0)).isEqualTo(adjustedMessageConverters.get(0));
    assertThat(originalMessageConverters.get(1)).isNotEqualTo(adjustedMessageConverters.get(1));
    assertThat(originalMessageConverters.get(2)).isEqualTo(adjustedMessageConverters.get(2));
  }

  /**
   * Asserts that the ProblemsJson MessageConverter does not get re-wrapped with a new ObjectMapper.
   */
  @Test
  public void testExtendMessageConvertersWithMultipleTypesOfHttpConverter() {
    final List<HttpMessageConverter<?>> originalMessageConverters = configureInitialMessageConvertersWithProblems();

    // Copy the list.
    final List<HttpMessageConverter<?>> adjustedMessageConverters = new ArrayList<>(originalMessageConverters);
    springConnectorWebMvc.extendMessageConverters(adjustedMessageConverters);

    assertThat(originalMessageConverters.size()).isEqualTo(4);
    assertThat(adjustedMessageConverters.size()).isEqualTo(4);

    assertThat(originalMessageConverters.get(0)).isEqualTo(adjustedMessageConverters.get(0));
    // This HttpMessageConverter should not be different
    assertThat(originalMessageConverters.get(1)).isEqualTo(adjustedMessageConverters.get(1));
    // This HttpMessageConverter SHOULD be different in order to use the correct default ObjectMapper.
    assertThat(originalMessageConverters.get(2)).isNotEqualTo(adjustedMessageConverters.get(2));
    assertThat(originalMessageConverters.get(3)).isEqualTo(adjustedMessageConverters.get(3));
  }

  //////////////////
  // Private Helpers
  //////////////////

  private List<HttpMessageConverter<?>> configureInitialMessageConvertersWithoutProblems() {
    List<HttpMessageConverter<?>> messageConverters = Lists.newArrayList();

    // For any byte[] payloads (e.g., `/settlements`)
    ByteArrayHttpMessageConverter octetStreamConverter = new ByteArrayHttpMessageConverter();
    octetStreamConverter.setSupportedMediaTypes(Lists.newArrayList(APPLICATION_OCTET_STREAM));
    messageConverters.add(octetStreamConverter);

    messageConverters.add(new MappingJackson2HttpMessageConverter(ObjectMapperFactory.create()));
    messageConverters.add(new OerPreparePacketHttpMessageConverter(InterledgerCodecContextFactory.oer()));

    return messageConverters;
  }

  private List<HttpMessageConverter<?>> configureInitialMessageConvertersWithProblems() {
    List<HttpMessageConverter<?>> messageConverters = configureInitialMessageConvertersWithoutProblems();

    // Put this converter into index 1 so it comes before the other HttpMessageConverter, which matches
    // what happens in springConnectorWebMvc.configureMessageConverters()
    messageConverters.add(1, springConnectorWebMvc.constructProblemsJsonConverter());

    return messageConverters;
  }

}
