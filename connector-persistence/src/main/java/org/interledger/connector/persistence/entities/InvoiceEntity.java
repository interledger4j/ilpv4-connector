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
  @Column(name = "INVOICE_ID") // Hibernate treats this as unique, but Liquibase is explicit about uniqueness.
  private String invoiceId;

  @Column(name = "INVOICE_URL")
  private String invoiceUrl;

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

  @Column(name = "FINALIZED_AT")
  private Instant finalizedAt;

  @Column(name = "PAYMENT_NETWORK")
  private String paymentNetwork;

  @Column(name = "PAYMENT_ID")
  private String paymentId;

  /**
   * For Hibernate
   */
  public InvoiceEntity() {}

  public InvoiceEntity(final Invoice invoice) {
    Objects.requireNonNull(invoice);
    this.accountId = invoice.accountId().orElse("");
    this.amount = invoice.amount().longValue();
    this.assetCode = invoice.assetCode();
    this.assetScale = invoice.assetScale();
    this.description = invoice.description();
    this.expiresAt = invoice.expiresAt();
    this.finalizedAt = invoice.finalizedAt();
    this.invoiceId = invoice.id().value();
    this.received = invoice.received().longValue();
    this.subject = invoice.subject();
    this.paymentNetwork = invoice.paymentNetwork().toString();
    this.paymentId = invoice.paymentId();
    this.invoiceUrl = invoice.invoiceUrl().toString();
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

  public Instant getFinalizedAt() {
    return finalizedAt;
  }

  public void setFinalizedAt(Instant finalizedAt) {
    this.finalizedAt = finalizedAt;
  }

  public String getPaymentNetwork() {
    return paymentNetwork;
  }

  public void setPaymentNetwork(String paymentNetwork) {
    this.paymentNetwork = paymentNetwork;
  }

  public String getPaymentId() {
    return paymentId;
  }

  public void setPaymentId(String paymentId) {
    this.paymentId = paymentId;
  }

  public String getInvoiceUrl() {
    return invoiceUrl;
  }

  public void setInvoiceUrl(String invoiceUrl) {
    this.invoiceUrl = invoiceUrl;
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
