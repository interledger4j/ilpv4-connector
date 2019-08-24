package org.interledger.connector.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.interledger.connector.jackson.modules.AccountIdModule;
import org.interledger.connector.jackson.modules.HttpUrlModule;
import org.interledger.connector.jackson.modules.LinkIdModule;
import org.interledger.connector.jackson.modules.LinkTypeModule;
import org.interledger.connector.jackson.modules.SettlementAccountIdModule;
import org.zalando.problem.ProblemModule;
import org.zalando.problem.violations.ConstraintViolationProblemModule;

/**
 * A factory for constructing instances of {@link ObjectMapper} for all connector components.
 */
public class ObjectMapperFactory {

  public static ObjectMapper create() {
    final ObjectMapper objectMapper = new ObjectMapper()
      .registerModule(new Jdk8Module())
      .registerModule(new HttpUrlModule())
      .registerModule(new JavaTimeModule())
      .registerModule(new GuavaModule())
      .registerModule(new ProblemModule())
      .registerModule(new ConstraintViolationProblemModule())
      .registerModule(new AccountIdModule())
      .registerModule(new SettlementAccountIdModule())
      .registerModule(new LinkIdModule())
      .registerModule(new LinkTypeModule());

    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    objectMapper.configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, true);
    objectMapper.configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true);


    return objectMapper;
  }
}
