@TypeDefs
  ({
    @TypeDef(
      name=AccountIdType.TYPE,
      typeClass= AccountIdType.class,
      defaultForType = AccountId.class
    )
  })
package org.interledger.connector.persistence.entities;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.persistence.types.AccountIdType;

import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;