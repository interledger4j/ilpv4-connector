package org.interledger.connector.persistence.types;

import org.interledger.connector.accounts.AccountId;

import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;

/**
 * Hibernate type for {@link AccountId}.
 */
public class AccountIdType extends AbstractSingleColumnStandardBasicType<AccountId> {

  public static final String TYPE = "accountId";

  public AccountIdType() {
    super( VarcharTypeDescriptor.INSTANCE, AccountIdTypeDescriptor.INSTANCE );
  }

  @Override
  public String getName() {
    return TYPE;
  }

  public static class AccountIdTypeDescriptor extends AbstractTypeDescriptor<AccountId> {
    public static final AccountIdTypeDescriptor INSTANCE = new AccountIdTypeDescriptor();

    public AccountIdTypeDescriptor() {
      super( AccountId.class );
    }

    @Override
    public AccountId fromString(String string) {
      return AccountId.of(string);
    }

    public <X> X unwrap(AccountId value, Class<X> type, WrapperOptions options) {
      if ( value == null ) {
        return null;
      }
      if (String.class.isAssignableFrom(type)) {
        return (X) value.value();
      }
      throw unknownUnwrap(type);
    }

    public <X> AccountId wrap(X value, WrapperOptions options) {
      if ( value == null ) {
        return null;
      }
      if ( String.class.isInstance( value ) ) {
        return AccountId.of((String) value);
      }
      throw unknownWrap( value.getClass() );
    }
  }

}
