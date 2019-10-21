package org.interledger.connector.jackson.modules;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.accounts.AccountAlreadyExistsProblem;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountNotFoundProblem;
import org.interledger.connector.jackson.ObjectMapperFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

public class ProblemSerializerTest {

  private ObjectMapper objectMapper;

  @Before
  public void setUp() {
    objectMapper = ObjectMapperFactory.create();
  }

  @Test
  public void serializeAccountAlreadyExists() throws Exception {
    AccountAlreadyExistsProblem existsProblem = new AccountAlreadyExistsProblem(AccountId.of("123"));
    final String actual = objectMapper.writeValueAsString(existsProblem);
    String expected = "{\"accountId\":\"123\"," +
        "\"type\":\"https://errors.interledger.org/accounts/account-already-exists\"," +
        "\"title\":\"Account Already Exists (`123`)\"," +
        "\"status\":409}";

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void serializeAccountNotFound() throws Exception {
    AccountNotFoundProblem existsProblem = new AccountNotFoundProblem(AccountId.of("123"));
    final String actual = objectMapper.writeValueAsString(existsProblem);
    String expected = "{\"accountId\":\"123\"," +
        "\"type\":\"https://errors.interledger.org/accounts/account-not-found\"," +
        "\"title\":\"Account Not Found (`123`)\"," +
        "\"status\":404}";

    assertThat(actual).isEqualTo(expected);
  }

}
