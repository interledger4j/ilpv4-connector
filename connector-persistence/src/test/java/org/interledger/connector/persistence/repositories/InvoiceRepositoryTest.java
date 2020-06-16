package org.interledger.connector.persistence.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.persistence.config.ConnectorPersistenceConfig;
import org.interledger.connector.persistence.converters.InvoiceEntityConverter;
import org.interledger.connector.persistence.entities.InvoiceEntity;
import org.interledger.connector.persistence.util.SampleObjectUtils;
import org.interledger.openpayments.Invoice;
import org.interledger.openpayments.PayIdAccountId;

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
  ConnectorPersistenceConfig.class, InvoiceRepositoryTest.TestPersistenceConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
@AutoConfigureEmbeddedDatabase
public class InvoiceRepositoryTest {

  @Autowired
  private InvoicesRepository invoicesRepository;

  @Test
  public void saveAndGetIlpInvoice() {
    Invoice invoice = SampleObjectUtils.createNewIlpInvoice();
    saveAndGetInvoice(invoice);
  }

  @Test
  public void saveAndGetXrpInvoice() {
    Invoice invoice = SampleObjectUtils.createNewXrpInvoice();
    saveAndGetInvoice(invoice);
  }

  @Test
  public void saveAndGetIlpInvoiceWithWrongAccountIdYieldsEmpty() {
    Invoice invoice = SampleObjectUtils.createNewIlpInvoice();
    Invoice saved = invoicesRepository.saveInvoice(invoice);
    Optional<InvoiceEntity> fetched = invoicesRepository
      .findByInvoiceIdAndAccountId(invoice.id(), PayIdAccountId.of(invoice.accountId() + "fakeAccount"));
    assertThat(fetched).isEmpty();
  }

  private void saveAndGetInvoice(Invoice invoice) {
    Invoice saved = invoicesRepository.saveInvoice(invoice);
    assertThat(saved).isNotNull().isEqualToIgnoringGivenFields(invoice,
      "id", "createdAt", "updatedAt", "ownerAccountUrl");
    Optional<Invoice> fetched = invoicesRepository.findInvoiceByInvoiceIdAndAccountId(invoice.id(), saved.accountId());
    assertThat(fetched).isNotEmpty().get().isEqualTo(saved);
  }

  @Configuration("application.yml")
  public static class TestPersistenceConfig {

    ////////////////////////
    // SpringConverters
    ////////////////////////

    @Autowired
    private InvoiceEntityConverter invoiceEntityConverter;

    @Bean
    public ObjectMapper objectMapper() {
      return new ObjectMapper();
    }

    @Bean
    public ConfigurableConversionService conversionService() {
      ConfigurableConversionService conversionService = new DefaultConversionService();
      conversionService.addConverter(invoiceEntityConverter);
      return conversionService;
    }
  }

}
