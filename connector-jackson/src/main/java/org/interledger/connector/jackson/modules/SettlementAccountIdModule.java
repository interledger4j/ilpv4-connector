package org.interledger.connector.jackson.modules;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.interledger.connector.accounts.SettlementEngineAccountId;


/**
 * A Jackson {@link SimpleModule} for serializing and deserializing instances of {@link SettlementEngineAccountId}.
 */
public class SettlementAccountIdModule extends SimpleModule {

  private static final String NAME = "SettlementAccountIdModule";

  /**
   * No-args Constructor.
   */
  public SettlementAccountIdModule() {
    super(
      NAME,
      new Version(1, 0, 0, null, "org.interledger.connector",
        "settlement-account-id")
    );

    addSerializer(SettlementEngineAccountId.class, new SettlementAccountIdSerializer());
    addDeserializer(SettlementEngineAccountId.class, new SettlementAccountIdDeserializer());
  }
}
