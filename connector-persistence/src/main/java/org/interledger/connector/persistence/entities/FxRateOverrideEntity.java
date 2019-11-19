package org.interledger.connector.persistence.entities;

import static org.interledger.connector.persistence.entities.DataConstants.ColumnNames.ASSET_CODE_IDX_COLUMNS;
import static org.interledger.connector.persistence.entities.DataConstants.IndexNames.FX_RATE_OVERRIDES_IDX;
import static org.interledger.connector.persistence.entities.DataConstants.TableNames.FX_RATE_OVERRIDES;

import org.interledger.connector.fxrates.FxRateOverride;

import org.hibernate.annotations.NaturalId;

import java.math.BigDecimal;
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
@Table(name = FX_RATE_OVERRIDES, indexes = {
    @Index(name = FX_RATE_OVERRIDES_IDX, columnList = ASSET_CODE_IDX_COLUMNS)
})
public class FxRateOverrideEntity extends AbstractEntity {

  @Id
  @GeneratedValue
  @Column(name = "ID")
  private Long id;

  @NaturalId
  @Column(name = "ASSET_CODE_KEY") // Hibernate treats this as unique, but Liquibase is explicit about uniqueness.
  private String assetCodeKey;

  @Column(name = "ASSET_CODE_FROM")
  private String assetCodeFrom;

  @Column(name = "ASSET_CODE_TO")
  private String assetCodeTo;

  @Column(name = "RATE")
  private BigDecimal rate;

  /**
   * For Hibernate....
   */
  FxRateOverrideEntity() {
  }

  public FxRateOverrideEntity(final FxRateOverride rateOverride) {
    Objects.requireNonNull(rateOverride);
    this.id = rateOverride.id();
    this.assetCodeKey = rateOverride.assetCodeFrom() + "-" + rateOverride.assetCodeTo();
    this.assetCodeFrom = rateOverride.assetCodeFrom();
    this.assetCodeTo = rateOverride.assetCodeTo();
    this.rate = rateOverride.rate();
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getAssetCodeKey() {
    return assetCodeKey;
  }

  public void setAssetCodeKey(String assetCodeKey) {
    this.assetCodeKey = assetCodeKey;
  }

  public String getAssetCodeFrom() {
    return assetCodeFrom;
  }

  public void setAssetCodeFrom(String assetCodeFrom) {
    this.assetCodeFrom = assetCodeFrom;
  }

  public String getAssetCodeTo() {
    return assetCodeTo;
  }

  public void setAssetCodeTo(String assetCodeTo) {
    this.assetCodeTo = assetCodeTo;
  }

  public BigDecimal getRate() {
    return rate;
  }

  public void setRate(BigDecimal rate) {
    this.rate = rate;
  }

  /**
   * Overridden to use natural identifier.
   *
   * @see "https://vladmihalcea.com/the-best-way-to-implement-equals-hashcode-and-tostring-with-jpa-and-hibernate/"
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    FxRateOverrideEntity that = (FxRateOverrideEntity) o;
    return Objects.equals(getAssetCodeKey(), that.getAssetCodeKey());
  }

  /**
   * Return the hashcode of the natural identifier.
   *
   * @return
   *
   * @see "https://vladmihalcea.com/the-best-way-to-implement-equals-hashcode-and-tostring-with-jpa-and-hibernate/"
   */
  @Override
  public int hashCode() {
    return Objects.hash(getAssetCodeKey());
  }

}
