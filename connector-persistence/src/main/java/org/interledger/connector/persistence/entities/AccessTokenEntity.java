package org.interledger.connector.persistence.entities;

import static org.interledger.connector.persistence.entities.DataConstants.ColumnNames.ACCOUNT_TOKEN_IDX_COLUMNS;
import static org.interledger.connector.persistence.entities.DataConstants.IndexNames.ACCESS_TOKENS_ACCT_ID_IDX;
import static org.interledger.connector.persistence.entities.DataConstants.TableNames.ACCESS_TOKENS;

import org.interledger.connector.accounts.AccountId;

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
@Table(name = ACCESS_TOKENS, indexes = {
  @Index(name = ACCESS_TOKENS_ACCT_ID_IDX, columnList = ACCOUNT_TOKEN_IDX_COLUMNS)
})
@SuppressWarnings( {"PMD"})
public class AccessTokenEntity extends AbstractEntity {

  @Id
  @GeneratedValue
  @Column(name = "ID")
  private Long id;

  @Column(name = "ACCOUNT_ID")
  private AccountId accountId;

  @Column(name = "ENCRYPTED_TOKEN")
  private String encryptedToken;

  public AccountId getAccountId() {
    return accountId;
  }

  public void setAccountId(AccountId accountId) {
    this.accountId = accountId;
  }

  public Long getId() {
    return this.id;
  }

  public String getEncryptedToken() {
    return encryptedToken;
  }

  public void setEncryptedToken(String encryptedToken) {
    this.encryptedToken = encryptedToken;
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

    AccessTokenEntity that = (AccessTokenEntity) o;
    return Objects.equals(getId(), that.getId());
  }

  /**
   * Return the hashcode of the natural identifier.
   *
   * @return An integer representing the natural identifier for this entity.
   *
   * @see "https://vladmihalcea.com/the-best-way-to-implement-equals-hashcode-and-tostring-with-jpa-and-hibernate/"
   */
  @Override
  public int hashCode() {
    return Objects.hash(getId());
  }

  @Override
  public String toString() {
    return "AccessTokenEntity{" +
      "id=" + id +
      ", accountId='" + accountId + '\'' +
      ", encryptedToken='" + encryptedToken + '\'' +
      ", createdAt=" + getCreatedDate() +
      '}';
  }
}


