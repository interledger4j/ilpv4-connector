package org.interledger.connector.settlement.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.math.BigInteger;

public class InitiateSettlementRequestTest {

  @Test
  public void serialize() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    String serialized = objectMapper.writeValueAsString(InitiateSettlementRequest.builder()
        .connectorAccountScale(3)
        .requestedSettlementAmount(BigInteger.TEN)
        .build());
    assertThat(serialized).isEqualTo("{\"scale\":3,\"amount\":\"10\"}");
  }
}
