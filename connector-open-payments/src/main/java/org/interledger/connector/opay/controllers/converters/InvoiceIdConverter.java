package org.interledger.connector.opay.controllers.converters;

import org.interledger.connector.opay.InvoiceId;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;

import java.util.UUID;

public class InvoiceIdConverter implements Converter<String, InvoiceId> {
  @Override
  public InvoiceId convert(String stringId) {
    return InvoiceId.of(UUID.fromString(stringId));
  }
}
