package org.interledger.ilpv4.connector.persistence;

import org.interledger.connector.accounts.AccountRelationship;

import javax.persistence.AttributeConverter;

/**
 * An {@link AttributeConverter} for storing a {@link AccountRelationship}.
 */
public class AccountRelationshipConverter implements AttributeConverter<AccountRelationship, Integer> {

  @Override
  public Integer convertToDatabaseColumn(AccountRelationship accountRelationship) {
    return accountRelationship == null ? null : accountRelationship.getWeight();
  }

  @Override
  public AccountRelationship convertToEntityAttribute(Integer weight) {
    return weight == null ? null : AccountRelationship.fromWeight(weight);
  }

}