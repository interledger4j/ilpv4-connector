package org.interledger.connector.jackson.modules;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.SettlementEngineAccountId;

import java.io.IOException;

/**
 * Jackson deserializer for {@link SettlementEngineAccountId}.
 */
public class SettlementAccountIdDeserializer extends StdDeserializer<SettlementEngineAccountId> {

  protected SettlementAccountIdDeserializer() {
    super(AccountId.class);
  }

  @Override
  public SettlementEngineAccountId deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
    return SettlementEngineAccountId.of(jsonParser.getText());
  }
}
