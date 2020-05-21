package org.interledger.connector.persistence.entities;

import static org.interledger.connector.persistence.entities.DataConstants.TableNames.INVOICE_PAYMENTS;

import org.interledger.connector.opa.model.Payment;

import org.hibernate.annotations.NaturalId;

import java.math.BigInteger;
import java.util.Objects;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Access(AccessType.FIELD)
@Table(name = INVOICE_PAYMENTS)
public class PaymentEntity extends AbstractEntity {

  @Id
  @Column(name = "ID")
  private Long id;

  @Column(name = "CORRELATION_ID")
  private String correlationId;

  @NaturalId
  @Column(name = "PAYMENT_ID")
  private String paymentId;

  @Column(name = "SOURCE_ADDRESS")
  private String sourceAddress;

  @Column(name = "DESTINATION_ADDRESS")
  private String destinationAddress;

  @Column(name = "AMOUNT")
  private BigInteger amount;

  @Column(name = "ASSET_CODE")
  private String assetCode;

  @Column(name = "ASSET_SCALE")
  private int assetScale;

  public PaymentEntity() { }

  public PaymentEntity(final Payment payment) {
    Objects.requireNonNull(payment);
    this.correlationId = payment.correlationId().orElseThrow(() -> new IllegalArgumentException("PaymentEntity must have a correlationId."));
    this.paymentId = payment.paymentId().value();
    this.sourceAddress = payment.sourceAddress();
    this.destinationAddress = payment.destinationAddress();
    this.amount = payment.amount().bigIntegerValue();
    this.assetCode = payment.denomination().assetCode();
    this.assetScale = payment.denomination().assetScale();
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public void setCorrelationId(String correlationId) {
    this.correlationId = correlationId;
  }

  public String getPaymentId() {
    return paymentId;
  }

  public void setPaymentId(String paymentId) {
    this.paymentId = paymentId;
  }

  public String getSourceAddress() {
    return sourceAddress;
  }

  public void setSourceAddress(String sourceAddress) {
    this.sourceAddress = sourceAddress;
  }

  public String getDestinationAddress() {
    return destinationAddress;
  }

  public void setDestinationAddress(String destinationAddress) {
    this.destinationAddress = destinationAddress;
  }

  public BigInteger getAmount() {
    return amount;
  }

  public void setAmount(BigInteger amount) {
    this.amount = amount;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PaymentEntity)) {
      return false;
    }
    PaymentEntity that = (PaymentEntity) o;
    return getPaymentId().equals(that.getPaymentId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getPaymentId());
  }
}
