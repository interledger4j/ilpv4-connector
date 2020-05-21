package org.interledger.connector.persistence.converters;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.Payment;
import org.interledger.connector.persistence.entities.InvoiceEntity;
import org.interledger.connector.persistence.entities.PaymentEntity;
import org.interledger.connector.persistence.util.SampleObjectUtils;

import org.junit.Test;

public class PaymentEntityConverterTest {

  @Test
  public void convertIlpPayment() {
    PaymentEntityConverter converter = new PaymentEntityConverter();

    Payment payment = SampleObjectUtils.createNewIlpPayment();

    PaymentEntity entity = new PaymentEntity(payment);
    Payment converted = converter.convert(entity);

    assertThat(converted).isEqualToIgnoringGivenFields(payment, "createdAt", "modifiedAt");
    assertThat(entity).isEqualTo(new PaymentEntity(payment));
  }

  @Test
  public void convertXrpPayment() {
    PaymentEntityConverter converter = new PaymentEntityConverter();

    Payment payment = SampleObjectUtils.createNewXrpPayment();

    PaymentEntity entity = new PaymentEntity(payment);
    Payment converted = converter.convert(entity);

    assertThat(converted).isEqualToIgnoringGivenFields(payment, "createdAt", "modifiedAt");
    assertThat(entity).isEqualTo(new PaymentEntity(payment));
  }
}
