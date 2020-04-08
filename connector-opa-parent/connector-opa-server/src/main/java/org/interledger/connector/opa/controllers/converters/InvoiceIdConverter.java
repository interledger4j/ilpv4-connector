package org.interledger.connector.opa.controllers.converters;


import org.interledger.connector.opa.model.InvoiceId;

import org.springframework.core.convert.converter.Converter;

public class InvoiceIdConverter implements Converter<String, InvoiceId> {
  @Override
  public InvoiceId convert(String stringId) {
    return InvoiceId.of(stringId);
  }
}
