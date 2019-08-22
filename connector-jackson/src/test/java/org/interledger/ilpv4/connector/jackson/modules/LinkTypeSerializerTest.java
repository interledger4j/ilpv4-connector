package org.interledger.ilpv4.connector.jackson.modules;

import org.interledger.connector.link.LinkType;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Unit tests for {@link LinkTypeSerializer}.
 */
public class LinkTypeSerializerTest extends AbstractIdTest {

  private LinkType LINK_TYPE = LinkType.of(UUID.randomUUID().toString());

  @Test
  public void shouldSerialize() throws IOException {
    final String actual = objectMapper.writeValueAsString(LINK_TYPE);
    assertThat(actual, is("\"" + LINK_TYPE.value() + "\""));
  }

  @Test
  public void shouldSerializeInContainer() throws IOException {
    final LinkTypeContainer expectedContainer = ImmutableLinkTypeContainer.builder()
      .linkType(LINK_TYPE)
      .build();

    final String actualJson = objectMapper.writeValueAsString(expectedContainer);

    assertThat(actualJson, is("{\"link_type\":\"" + LINK_TYPE.value() + "\"}"));
  }
}