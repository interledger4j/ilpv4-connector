package org.interledger.connector.jackson.modules;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.accounts.AccountAlreadyExistsProblem;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountNotFoundProblem;
import org.interledger.connector.jackson.ObjectMapperFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;
import org.zalando.problem.StatusType;
import org.zalando.problem.ThrowableProblem;

import java.net.URI;

/**
 * Unit tests to validate that Problems marshal to/from JSON using Jackson.
 */
public class ProblemSerializerTest {

  private ObjectMapper objectMapper;

  @Before
  public void setUp() {
    objectMapper = ObjectMapperFactory.createObjectMapperForProblemsJson();
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
    AccountNotFoundProblem problem = new AccountNotFoundProblem(AccountId.of("123"));
    final String actual = objectMapper.writeValueAsString(problem);
    String expected = "{\"accountId\":\"123\"," +
      "\"type\":\"https://errors.interledger.org/accounts/account-not-found\"," +
      "\"title\":\"Account Not Found (`123`)\"," +
      "\"status\":404}";

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void serializeThrowableProblemWithInternalServerError() throws Exception {
    ThrowableProblem problem = new ThrowableProblem() {
      @Override
      public StatusType getStatus() {
        return Status.INTERNAL_SERVER_ERROR;
      }
    };

    final String actual = objectMapper.writeValueAsString(problem);

    String expected = "{"
      + "\"status\":500"
      + "}";
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void serializeProblemWithInternalServerError() throws Exception {
    Problem problem = new Problem() {
      @Override
      public StatusType getStatus() {
        return Status.INTERNAL_SERVER_ERROR;
      }
    };

    final String actual = objectMapper.writeValueAsString(problem);

    String expected = "{"
      + "\"status\":500"
      + "}";
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void serializeProblemWithNull() throws Exception {
    AbstractThrowableProblem problem = new AbstractThrowableProblem(
      URI.create("http://test.com/problem-without-a-status"),
      "problem without a status") {
      @Override
      public URI getType() {
        return super.getType();
      }
    };
    final String actual = objectMapper.writeValueAsString(problem);
    String expected = "{"
      + "\"type\":\"http://test.com/problem-without-a-status\","
      + "\"title\":\"problem without a status\""
      + "}";
    assertThat(actual).isEqualTo(expected);
  }

}
