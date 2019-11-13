package org.interledger.connector.persistence.entities;

import static org.interledger.connector.persistence.entities.DataConstants.ColumnNames.ADDRESS_PREFIX;
import static org.interledger.connector.persistence.entities.DataConstants.IndexNames.STATIC_ROUTES_IDX;
import static org.interledger.connector.persistence.entities.DataConstants.TableNames.STATIC_ROUTES;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.routing.StaticRoute;
import org.interledger.core.InterledgerAddressPrefix;

import org.hibernate.annotations.NaturalId;

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
@Table(name = STATIC_ROUTES, indexes = {
    @Index(name = STATIC_ROUTES_IDX, columnList = ADDRESS_PREFIX)
})
public class StaticRouteEntity extends AbstractEntity {

  @Id
  @GeneratedValue
  @Column(name = "ID")
  private Long id;

  // this is really the interledger prefix
  @NaturalId
  @Column(name = "ADDRESS_PREFIX") // Hibernate treats this as unique, but Liquibase is explicit about uniqueness.
  private String addressPrefix;

  @Column(name = "ACCOUNT_ID")
  private String accountId;


  // hibernate
  StaticRouteEntity() {}

  public StaticRouteEntity(final StaticRoute staticRoute) {
    Objects.requireNonNull(staticRoute);
    this.addressPrefix = staticRoute.routePrefix().getValue();
    this.accountId = staticRoute.nextHopAccountId().value();
  }

  public Long getId() {
    return this.id;
  }

  public String getAddressPrefix() {
    return addressPrefix;
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public AccountId getBoxedAccountId() {
    return AccountId.of(this.accountId);
  }

  public InterledgerAddressPrefix getPrefix() {
    return InterledgerAddressPrefix.of(this.addressPrefix);
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

    StaticRouteEntity that = (StaticRouteEntity) o;
    return Objects.equals(getAddressPrefix(), that.getAddressPrefix());
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
    return Objects.hash(getAddressPrefix());
  }

}
