package org.interledger.connector.persistence.converters;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.persistence.entities.InvoiceEntity;
import org.interledger.connector.persistence.util.SampleObjectUtils;

import org.junit.Test;

public class InvoiceEntityConverterTest {

  @Test
  public void convert() {
    InvoiceEntityConverter converter = new InvoiceEntityConverter();

    Invoice invoice = SampleObjectUtils.createNewIlpInvoice();

    InvoiceEntity entity = new InvoiceEntity(invoice);
    Invoice converted = converter.convert(entity);

    assertThat(converted).isEqualToIgnoringGivenFields(invoice, "createdAt", "updatedAt");
    assertThat(entity).isEqualTo(new InvoiceEntity(converted));
  }
}
