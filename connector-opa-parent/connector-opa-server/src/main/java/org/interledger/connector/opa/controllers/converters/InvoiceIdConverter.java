package org.interledger.connector.opa.controllers.converters;


import org.interledger.connector.opa.model.InvoiceId;

import org.springframework.core.convert.converter.Converter;

import java.util.UUID;

public class InvoiceIdConverter implements Converter<String, InvoiceId> {
  @Override
  public InvoiceId convert(String stringId) {
    return InvoiceId.of(UUID.fromString(stringId));
  }
}
