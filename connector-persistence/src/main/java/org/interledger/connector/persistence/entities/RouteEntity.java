package org.interledger.connector.persistence.entities;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.routing.Route;

import org.hibernate.annotations.NaturalId;

import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

public class RouteEntity extends AbstractEntity {

  @Id
  @GeneratedValue
  @Column(name = "ID")
  private Long id;

  @NaturalId
  @Column(name = "NATURAL_ID") // Hibernate treats this as unique, but Liquibase is explicit about uniqueness.
  private String naturalId;

  @Column(name = "PREFIX")
  private String prefix;


  // hibernate
  RouteEntity() {}

  public RouteEntity(final Route route) {
    Objects.requireNonNull(route);
    this.naturalId = route.nextHopAccountId().value();
    this.prefix = route.routePrefix().getValue();
  }

  public AccountId getAccountId() {
    return AccountId.of(getNaturalId());
  }

  public Long getId() {
    return this.id;
  }

  public String getNaturalId() {
    return naturalId;
  }

  public String getPrefix() {
    return prefix;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
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

    AccountSettingsEntity that = (AccountSettingsEntity) o;
    return Objects.equals(getNaturalId(), that.getNaturalId());
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
    return Objects.hash(getNaturalId());
  }

}
