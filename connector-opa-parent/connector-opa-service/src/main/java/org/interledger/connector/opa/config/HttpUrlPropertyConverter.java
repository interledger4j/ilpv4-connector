package org.interledger.connector.opa.config;

import okhttp3.HttpUrl;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * A {@link Converter} which converts a {@link String} to an {@link HttpUrl}.
 *
 * This will allow configuration class fields to be typed as {@link HttpUrl}.
 */
@Component
@ConfigurationPropertiesBinding
public class HttpUrlPropertyConverter implements Converter<String, HttpUrl> {
  @Override
  public HttpUrl convert(String property) {
    return HttpUrl.parse(property);
  }
}
