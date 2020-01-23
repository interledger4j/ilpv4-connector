package org.interledger.connector.jackson.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.jackson.ObjectMapperFactory;
import org.interledger.core.InterledgerAddressPrefix;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

public class InterledgerAddressPrefixTest {

  private static final InterledgerAddressPrefix ADDRESS_PREFIX = InterledgerAddressPrefix.of("test.out.the.object.mapper");
  private static ObjectMapper objectMapper = ObjectMapperFactory.create();

  @Test
  public void serializeAndDeserializeAddressPrefix() throws JsonProcessingException {
    String json = objectMapper.writeValueAsString(ADDRESS_PREFIX);
    InterledgerAddressPrefix deserializedPrefix = objectMapper.readValue(json, InterledgerAddressPrefix.class);
    assertThat(deserializedPrefix).isEqualTo(ADDRESS_PREFIX);
  }
}
