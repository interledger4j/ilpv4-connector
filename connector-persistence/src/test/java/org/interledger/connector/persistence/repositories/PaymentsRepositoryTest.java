package org.interledger.connector.persistence.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.Payment;
import org.interledger.connector.persistence.config.ConnectorPersistenceConfig;
import org.interledger.connector.persistence.converters.InvoiceEntityConverter;
import org.interledger.connector.persistence.converters.PaymentEntityConverter;
import org.interledger.connector.persistence.util.SampleObjectUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Optional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
  ConnectorPersistenceConfig.class, PaymentsRepositoryTest.TestPersistenceConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
@AutoConfigureEmbeddedDatabase
public class PaymentsRepositoryTest {

  @Autowired
  private PaymentsRepository paymentsRepository;

  @Test
  public void saveAndGetPaymentByPaymentId() {
    Payment payment = SampleObjectUtils.createNewIlpPayment();
    Payment saved = paymentsRepository.savePayment(payment);
    assertThat(saved).isNotNull().isEqualToIgnoringGivenFields(payment,
      "id", "createdAt", "modifiedAt");

    Optional<Payment> fetched = paymentsRepository.findPaymentByPaymentId(payment.paymentId());
    assertThat(fetched).isNotEmpty().get().isEqualTo(saved);
  }

  @Test
  public void saveAndGetPaymentByCorrelationId() {
    Payment payment = SampleObjectUtils.createNewIlpPayment();
    Payment saved = paymentsRepository.savePayment(payment);
    assertThat(saved).isNotNull().isEqualToIgnoringGivenFields(payment,
      "id", "createdAt", "modifiedAt");

    Optional<Payment> fetched = paymentsRepository.findPaymentByCorrelationId(payment.correlationId().get());
    assertThat(fetched).isNotEmpty().get().isEqualTo(saved);
  }

  @Configuration("application.yml")
  public static class TestPersistenceConfig {

    ////////////////////////
    // SpringConverters
    ////////////////////////

    @Autowired
    private PaymentEntityConverter paymentEntityConverter;

    @Bean
    public ObjectMapper objectMapper() {
      return new ObjectMapper();
    }

    @Bean
    public ConfigurableConversionService conversionService() {
      ConfigurableConversionService conversionService = new DefaultConversionService();
      conversionService.addConverter(paymentEntityConverter);
      return conversionService;
    }
  }

}
