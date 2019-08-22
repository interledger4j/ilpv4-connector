package org.interledger.connector.jackson.modules;

import org.interledger.connector.link.LinkId;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Unit tests for {@link LinkIdSerializer}.
 */
public class LinkIdSerializerTest extends AbstractIdTest {

  private LinkId LINK_ID = LinkId.of(UUID.randomUUID().toString());

  @Test
  public void shouldSerialize() throws IOException {
    final String actual = objectMapper.writeValueAsString(LINK_ID);
    assertThat(actual, is("\"" + LINK_ID.value() + "\""));
  }

  @Test
  public void shouldSerializeInContainer() throws IOException {
    final LinkIdContainer expectedContainer = ImmutableLinkIdContainer.builder()
      .linkId(LINK_ID)
      .build();

    final String actualJson = objectMapper.writeValueAsString(expectedContainer);

    assertThat(actualJson, is("{\"link_id\":\"" + LINK_ID.value() + "\"}"));
  }
}
