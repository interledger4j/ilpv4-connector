package org.interledger.connector.opa.persistence.types;

import org.interledger.connector.opa.model.InvoiceId;

import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;

import java.util.UUID;

/**
 * Hibernate type for {@link InvoiceId}.
 */
public class InvoiceIdType extends AbstractSingleColumnStandardBasicType<InvoiceId> {

  public static final String TYPE = "InvoiceId";

  public InvoiceIdType() {
    super( VarcharTypeDescriptor.INSTANCE, InvoiceIdTypeDescriptor.INSTANCE );
  }

  @Override
  public String getName() {
    return TYPE;
  }

  public static class InvoiceIdTypeDescriptor extends AbstractTypeDescriptor<InvoiceId> {
    public static final InvoiceIdTypeDescriptor INSTANCE = new InvoiceIdTypeDescriptor();

    public InvoiceIdTypeDescriptor() {
      super( InvoiceId.class );
    }

    @Override
    public InvoiceId fromString(String string) {
      return InvoiceId.of(UUID.fromString(string));
    }

    public <X> X unwrap(InvoiceId value, Class<X> type, WrapperOptions options) {
      if ( value == null ) {
        return null;
      }
      if (String.class.isAssignableFrom(type)) {
        return (X) value.value();
      }
      throw unknownUnwrap(type);
    }

    public <X> InvoiceId wrap(X value, WrapperOptions options) {
      if ( value == null ) {
        return null;
      }
      if ( String.class.isInstance( value ) ) {
        return InvoiceId.of(UUID.fromString((String) value));
      }
      throw unknownWrap( value.getClass() );
    }
  }

}
