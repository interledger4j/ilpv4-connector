package org.interledger.connector.persistence.entities;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;
import java.time.Instant;

/**
 * The super-class of all entities persisted by a Connector.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@SuppressWarnings({"PMD"})
public class AbstractEntity {

  @Column(name = "CREATED_DTTM", nullable = false, updatable = false)
  @CreatedDate
  private Instant createdDate;

  @Column(name = "MODIFIED_DTTM", nullable = false)
  @LastModifiedDate
  private Instant modifiedDate;

  @Version
  private Integer version;

  public Instant getCreatedDate() {
    return createdDate;
  }

  public Instant getModifiedDate() {
    return modifiedDate;
  }
}
