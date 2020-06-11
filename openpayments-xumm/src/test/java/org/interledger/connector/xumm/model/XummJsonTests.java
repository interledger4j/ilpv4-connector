package org.interledger.connector.xumm.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.jackson.ObjectMapperFactory;
import org.interledger.openpayments.ApproveMandateRequest;
import org.interledger.openpayments.MandateId;
import org.interledger.openpayments.SendXrpPaymentRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.primitives.UnsignedLong;
import org.junit.Test;

public class XummJsonTests {

  private static final ObjectMapper MAPPER = ObjectMapperFactory.createObjectMapperForProblemsJson();

  @Test
  public void customMetaWithSendXrpRequestSerialization() throws JsonProcessingException {

    CustomMeta original = CustomMeta.builder()
      .instruction("hello")
      .blob(SendXrpRequestWrapper.of(AccountId.of("account123"),
        SendXrpPaymentRequest.builder()
          .destinationAddress("12345")
          .correlationId("xyz")
          .amountInDrops(UnsignedLong.ONE)
          .build()
      ))
      .build();

    CustomMeta result = MAPPER.readValue(MAPPER.writeValueAsString(original), ImmutableCustomMeta.class);

    assertThat(result).isEqualTo(original);
  }

  @Test
  public void customMetaWithApproveMandateRequestSerialization() throws JsonProcessingException {

    AccountId account123 = AccountId.of("account123");
    CustomMeta original = CustomMeta.builder()
      .instruction("hello")
      .blob(ApproveMandateRequestWrapper.of(account123,
        ApproveMandateRequest.builder()
          .accountId(account123)
          .mandateId(MandateId.of("abc"))
          .memoToUser("hello")
          .build()
      ))
      .build();

    CustomMeta result = MAPPER.readValue(MAPPER.writeValueAsString(original), ImmutableCustomMeta.class);

    assertThat(result).isEqualTo(original);
  }

}
