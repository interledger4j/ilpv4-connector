@TypeDefs
  ({
    @TypeDef(
      name=AccountIdType.TYPE,
      typeClass= AccountIdType.class,
      defaultForType = AccountId.class
    ),
    @TypeDef(
      name= InvoiceIdType.TYPE,
      typeClass= InvoiceIdType.class,
      defaultForType = InvoiceId.class
    )
  })
package org.interledger.connector.persistence.entities;

import org.interledger.connector.accounts.AccountId;
import org.interledger.openpayments.InvoiceId;
import org.interledger.connector.persistence.types.AccountIdType;
import org.interledger.connector.persistence.types.InvoiceIdType;

import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
