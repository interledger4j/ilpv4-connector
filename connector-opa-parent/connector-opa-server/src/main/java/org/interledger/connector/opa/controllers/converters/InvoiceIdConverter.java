package org.interledger.connector.opa.controllers.converters;


import org.interledger.connector.opa.model.InvoiceId;

import okhttp3.HttpUrl;
import org.springframework.core.convert.converter.Converter;

/**
 * A {@link Converter} which converts a {@link String} to an {@link HttpUrl}.
 *
 * This will allow Spring controller parameters to be typed as {@link InvoiceId} once it is registered in
 * the WebMvc config.
 */
public class InvoiceIdConverter implements Converter<String, InvoiceId> {
  @Override
  public InvoiceId convert(String stringId) {
    return InvoiceId.of(stringId);
  }
}
