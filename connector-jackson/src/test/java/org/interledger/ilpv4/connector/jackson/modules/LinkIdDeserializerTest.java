package org.interledger.ilpv4.connector.jackson.modules;

import org.interledger.connector.link.LinkId;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Unit tests for {@link LinkIdDeserializer}.
 */
public class LinkIdDeserializerTest extends AbstractIdTest {

  private LinkId LINK_ID = LinkId.of(UUID.randomUUID().toString());

  @Test
  public void shouldDeserialize() throws IOException {
    final LinkId actual = objectMapper
      .readValue("\"" + LINK_ID.value() + "\"", LinkId.class);

    assertThat(actual, is(LINK_ID));
  }

  @Test
  public void shouldDeserializeInContainer() throws IOException {
    final LinkIdContainer expectedContainer = ImmutableLinkIdContainer.builder()
      .linkId(LINK_ID)
      .build();

    final LinkIdContainer actualContainer = objectMapper.readValue(
      "{\"link_id\":\"" + LINK_ID.value() + "\"}",
      LinkIdContainer.class
    );

    assertThat(actualContainer, is(expectedContainer));
  }

}