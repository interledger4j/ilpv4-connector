package org.interledger.connector.jackson.modules;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.SettlementEngineAccountId;

import java.io.IOException;

/**
 * Jackson serializer {@link SettlementEngineAccountId}.
 */
public class SettlementAccountIdSerializer extends StdScalarSerializer<SettlementEngineAccountId> {

  public SettlementAccountIdSerializer() {
    super(AccountId.class, false);
  }

  @Override
  public void serialize(SettlementEngineAccountId accountId, JsonGenerator gen, SerializerProvider provider) throws IOException {
    gen.writeString(accountId.value());
  }
}
