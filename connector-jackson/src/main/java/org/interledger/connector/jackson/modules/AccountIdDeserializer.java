package org.interledger.connector.jackson.modules;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.interledger.connector.accounts.AccountId;

import java.io.IOException;

/**
 * Jackson deserializer for {@link AccountId}.
 */
public class AccountIdDeserializer extends StdDeserializer<AccountId> {

  protected AccountIdDeserializer() {
    super(AccountId.class);
  }

  @Override
  public AccountId deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
    return AccountId.of(jsonParser.getText());
  }
}
