package org.interledger.ilpv4.connector.jackson.modules;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import okhttp3.HttpUrl;

import java.io.IOException;

/**
 * An extension of {@link SimpleModule} for registering with Jackson {@link ObjectMapper} to serialize instances of
 * {@link HttpUrl}.
 *
 * @see "http://wiki.fasterxml.com/JacksonMixInAnnotations"
 * @see "https://github.com/square/okhttp/blob/master/okhttp/src/main/java/com/squareup/okhttp/HttpUrl.java"
 */
public class HttpUrlModule extends SimpleModule {
  /**
   * No-args Constructor.
   */
  public HttpUrlModule() {
    super("HttpUrlModule", new Version(1, 0, 0, null));

    this.addSerializer(new ToStringSerializer(HttpUrl.class));
    this.addDeserializer(HttpUrl.class, new HttpUrlDeserializer());
  }

  /**
   * An extension of {@link FromStringDeserializer} that deserializes a JSON string into an instance of {@link
   * HttpUrl}.
   */
  public static class HttpUrlDeserializer extends FromStringDeserializer<HttpUrl> {
    /**
     * No-args Constructor.
     */
    public HttpUrlDeserializer() {
      super(HttpUrl.class);
    }

    @Override
    protected HttpUrl _deserialize(final String s, final DeserializationContext deserializationContext)
      throws IOException {
      return HttpUrl.parse(s);
    }
  }
}
