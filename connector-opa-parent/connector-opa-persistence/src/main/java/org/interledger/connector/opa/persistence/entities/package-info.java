@TypeDefs
  ({
    @TypeDef(
      name= InvoiceIdType.TYPE,
      typeClass= InvoiceIdType.class,
      defaultForType = InvoiceId.class
    )
  })
package org.interledger.connector.opa.persistence.entities;

import org.interledger.connector.opa.model.InvoiceId;
import org.interledger.connector.opa.persistence.types.InvoiceIdType;

import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;