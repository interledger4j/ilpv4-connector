package org.interledger.ilpv4.connector.jackson.modules;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.interledger.connector.link.LinkType;
import org.interledger.ilpv4.connector.jackson.ObjectMapperFactory;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Unit tests for {@link LinkTypeModule}.
 */
public class LinkTypeModuleTest extends AbstractIdTest {

  private static final LinkType LINK_TYPE = LinkType.of(UUID.randomUUID().toString());

  @Test
  public void shouldSerializeAndDeserialize() throws IOException {
    final LinkTypeContainer expectedContainer = ImmutableLinkTypeContainer.builder()
      .linkType(LINK_TYPE)
      .build();

    final String json = objectMapper.writeValueAsString(expectedContainer);
    final LinkTypeContainer actualContainer = objectMapper
      .readValue(json, LinkTypeContainer.class);

    assertThat(actualContainer, is(expectedContainer));
  }

  @Test
  public void shouldNotSerializeAndDeserialize() throws IOException {
    ObjectMapper objectMapperWithoutModule = ObjectMapperFactory.create();
    final LinkTypeContainer expectedContainer = ImmutableLinkTypeContainer.builder()
      .linkType(LINK_TYPE)
      .build();

    final String actualJson = objectMapperWithoutModule.writeValueAsString(expectedContainer);
    final LinkTypeContainer decodedJson = objectMapperWithoutModule.readValue(actualJson, LinkTypeContainer.class);
    assertThat(decodedJson, is(expectedContainer));
  }
}
