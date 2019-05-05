package org.interledger.ilpv4.connector.jackson.modules;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.interledger.connector.accounts.AccountId;


/**
 * A Jackson {@link SimpleModule} for serializing and deserializing instances of {@link AccountId}.
 */
public class AccountIdModule extends SimpleModule {

  private static final String NAME = "AccountIdModule";

  /**
   * No-args Constructor.
   */
  public AccountIdModule() {
    super(
      NAME,
      new Version(1, 0, 0, null, "org.interledger.ilpv4.connector",
        "account-id")
    );

    addSerializer(AccountId.class, new AccountIdSerializer());
    addDeserializer(AccountId.class, new AccountIdDeserializer());
  }
}