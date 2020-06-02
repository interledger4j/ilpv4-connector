package org.interledger.connector.persistence.entities;

import static org.interledger.connector.persistence.entities.DataConstants.ColumnNames.INVOICE_IDX_COLUMN_NAMES;
import static org.interledger.connector.persistence.entities.DataConstants.IndexNames.INVOICES_ID_IDX;
import static org.interledger.connector.persistence.entities.DataConstants.TableNames.INVOICES;

import org.interledger.connector.opa.model.Invoice;

import org.hibernate.annotations.NaturalId;

import java.time.Instant;
import java.util.Objects;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

@Entity
@Access(AccessType.FIELD)
@Table(name = INVOICES, indexes = {
  @Index(name = INVOICES_ID_IDX, columnList = INVOICE_IDX_COLUMN_NAMES)
})
@SuppressWarnings({"PMD"})
public class InvoiceEntity extends AbstractEntity {

  @Id
  @GeneratedValue
  @Column(name = "ID")
  private Long id;

  @NaturalId
  @Column(name = "INVOICE_ID")
  private String invoiceId;

  @Column(name = "INVOICE_URL")
  private String invoiceUrl;

  @Column(name = "ORIGINAL_INVOICE_URL")
  private String originalInvoiceUrl;

  @Column(name = "ACCOUNT_URL")
  private String accountUrl;

  @Column(name = "ACCOUNT_ID")
  private String accountId;

  @Column(name = "ASSET_CODE")
  private String assetCode;

  @Column(name = "ASSET_SCALE")
  private int assetScale;

  @Column(name = "AMOUNT")
  private Long amount;

  @Column(name = "RECEIVED")
  private Long received;

  @Column(name = "SUBJECT")
  private String subject;

  @Column(name = "DESCRIPTION")
  private String description;

  @Column(name = "EXPIRES_AT", nullable = false)
  private Instant expiresAt;

  @Column(name = "CORRELATION_ID")
  private String correlationId;

  /**
   * For Hibernate
   */
  public InvoiceEntity() {}

  public InvoiceEntity(final Invoice invoice) {
    Objects.requireNonNull(invoice);
    this.invoiceId = invoice.id().value();
    this.invoiceUrl = invoice.invoiceUrl().toString();
    this.originalInvoiceUrl = invoice.originalInvoiceUrl().toString();
    this.accountUrl = invoice.account().toString();
    this.accountId = invoice.accountId().value();
    this.amount = invoice.amount().longValue();
    this.assetCode = invoice.assetCode();
    this.assetScale = invoice.assetScale();
    this.description = invoice.description();
    this.expiresAt = invoice.expiresAt();
    this.received = invoice.received().longValue();
    this.subject = invoice.subject();
    this.correlationId = invoice.correlationId().value();
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getInvoiceId() {
    return invoiceId;
  }

  public void setInvoiceId(String invoiceId) {
    this.invoiceId = invoiceId;
  }

  public String getInvoiceUrl() {
    return invoiceUrl;
  }

  public void setInvoiceUrl(String invoiceUrl) {
    this.invoiceUrl = invoiceUrl;
  }

  public String getOriginalInvoiceUrl() {
    return originalInvoiceUrl;
  }

  public void setOriginalInvoiceUrl(String originalInvoiceUrl) {
    this.originalInvoiceUrl = originalInvoiceUrl;
  }

  public String getAccountUrl() {
    return accountUrl;
  }

  public void setAccountUrl(String accountUrl) {
    this.accountUrl = accountUrl;
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public String getAssetCode() {
    return assetCode;
  }

  public void setAssetCode(String assetCode) {
    this.assetCode = assetCode;
  }

  public int getAssetScale() {
    return assetScale;
  }

  public void setAssetScale(int assetScale) {
    this.assetScale = assetScale;
  }

  public Long getAmount() {
    return amount;
  }

  public void setAmount(Long amount) {
    this.amount = amount;
  }

  public Long getReceived() {
    return received;
  }

  public void setReceived(Long received) {
    this.received = received;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public void setCorrelationId(String correlationId) {
    this.correlationId = correlationId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof InvoiceEntity)) {
      return false;
    }
    InvoiceEntity that = (InvoiceEntity) o;
    return getInvoiceId().equals(that.getInvoiceId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getInvoiceId());
  }
}
