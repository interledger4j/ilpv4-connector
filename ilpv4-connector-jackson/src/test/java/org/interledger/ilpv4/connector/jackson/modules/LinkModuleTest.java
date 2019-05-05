package org.interledger.ilpv4.connector.jackson.modules;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.interledger.connector.link.LinkId;
import org.interledger.ilpv4.connector.jackson.ObjectMapperFactory;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Unit tests for {@link LinkIdModule}.
 */
public class LinkModuleTest extends AbstractIdTest {

  private static final LinkId LINK_ID = LinkId.of(UUID.randomUUID().toString());
  protected ObjectMapper objectMapperWithoutModule;

  @Before
  public void setUp() {
    objectMapperWithoutModule = ObjectMapperFactory.create();
    objectMapper.registerModule(new LinkIdModule());
  }

  @Test
  public void shouldSerializeAndDeserialize() throws IOException {
    final LinkIdContainer expectedContainer = ImmutableLinkIdContainer.builder()
      .linkId(LINK_ID)
      .build();

    final String json = objectMapper.writeValueAsString(expectedContainer);
    final LinkIdContainer actualContainer = objectMapper
      .readValue(json, LinkIdContainer.class);

    assertThat(actualContainer, is(expectedContainer));
  }

  @Test
  public void shouldNotSerializeAndDeserialize() throws IOException {
    final LinkIdContainer expectedContainer = ImmutableLinkIdContainer.builder()
      .linkId(LINK_ID)
      .build();

    final String actualJson = objectMapperWithoutModule.writeValueAsString(expectedContainer);
    final LinkIdContainer decodedJson = objectMapperWithoutModule.readValue(actualJson, LinkIdContainer.class);
    assertThat(decodedJson, is(expectedContainer));
  }
}