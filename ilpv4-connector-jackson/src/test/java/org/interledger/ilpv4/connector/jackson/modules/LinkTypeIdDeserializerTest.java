package org.interledger.ilpv4.connector.jackson.modules;

import org.interledger.connector.link.LinkType;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Unit tests for {@link LinkTypeDeserializer}.
 */
public class LinkTypeIdDeserializerTest extends AbstractIdTest {

  private LinkType LINK_TYPE = LinkType.of(UUID.randomUUID().toString());

  @Test
  public void shouldDeserialize() throws IOException {
    final LinkType actual = objectMapper
      .readValue("\"" + LINK_TYPE.value() + "\"", LinkType.class);

    assertThat(actual, is(LINK_TYPE));
  }

  @Test
  public void shouldDeserializeInContainer() throws IOException {
    final LinkTypeContainer expectedContainer = ImmutableLinkTypeContainer.builder()
      .linkType(LINK_TYPE)
      .build();

    final LinkTypeContainer actualContainer = objectMapper.readValue(
      "{\"link_type\":\"" + LINK_TYPE.value() + "\"}",
      LinkTypeContainer.class
    );

    assertThat(actualContainer, is(expectedContainer));
  }

}