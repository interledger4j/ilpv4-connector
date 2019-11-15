package org.interledger.connector.jackson.modules;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import okhttp3.HttpUrl;
import org.immutables.value.Value;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for the {@link HttpUrlModule}.
 */
public class HttpUrlModuleTest {
  private ObjectMapper objectMapper;

  @Before
  public void setUp() throws Exception {
    this.objectMapper = new ObjectMapper();
    this.objectMapper.registerModule(new HttpUrlModule());
  }

  @Test
  public void testSerializeDeserializeHttpUrl() throws Exception {
    final Earl earl = Earl.builder().href(HttpUrl.parse("https://api.example.com/foo")).build();

    final String serializedHrefUrl = this.objectMapper.writeValueAsString(earl);
    assertThat(serializedHrefUrl).isEqualTo("{\"href\":\"https://api.example.com/foo\"}");

    final Earl deserializedEarl = this.objectMapper.readValue(serializedHrefUrl, Earl.class);
    assertThat(deserializedEarl.href()).isEqualTo(earl.href());
  }

  @Test
  public void testSerializeDeserializeHttpUrlWeirdEncoding() throws Exception {
    final Earl earl = Earl.builder().href(HttpUrl.parse("https://api.example.com").newBuilder()
      .addEncodedPathSegment("foo").addEncodedPathSegment("http:%2F%2Fwww.cnn.com%2Fstory%2F123").build()).build();

    final String serializedHrefUrl = this.objectMapper.writeValueAsString(earl);
    assertThat(serializedHrefUrl).isEqualTo("{\"href\":\"https://api.example.com/foo/http:%2F%2Fwww.cnn.com%2Fstory%2F123\"}");

    final Earl deserializedEarl = this.objectMapper.readValue(serializedHrefUrl, Earl.class);
    assertThat(deserializedEarl.href()).isEqualTo(earl.href());
  }

  @Test
  public void testSerializeDeserializeHttpUrlMandarinEncoding() throws Exception {
    final Earl earl =
      Earl.builder().href(HttpUrl.parse("https://api.example.com").newBuilder().addEncodedPathSegment("foo")
        .addEncodedPathSegment("诶比西迪伊").build()).build();

    final String serializedHrefUrl = this.objectMapper.writeValueAsString(earl);
    assertThat(serializedHrefUrl).isEqualTo("{\"href\":\"https://api.example.com/foo/%E8%AF%B6%E6%AF%94%E8%A5%BF%E8%BF%AA%E4%BC%8A\"}");

    final Earl deserializedEarl = this.objectMapper.readValue(serializedHrefUrl, Earl.class);
    assertThat(deserializedEarl.href()).isEqualTo(earl.href());
  }

  /**
   * A class for testing the serialization of {@link HttpUrl}.
   */
  @Value.Immutable
  @JsonSerialize(as = ImmutableEarl.class)
  @JsonDeserialize(as = ImmutableEarl.class)
  public interface Earl {
    static ImmutableEarl.Builder builder() {
      return ImmutableEarl.builder();
    }

    HttpUrl href();
  }

}
