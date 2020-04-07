package org.interledger.connector.opay.jackson;

import org.interledger.connector.opay.jackson.modules.HttpUrlModule;
import org.interledger.connector.opay.jackson.modules.InvoiceIdModule;
import org.interledger.connector.opay.jackson.modules.PaymentPointerModule;
import org.interledger.quilt.jackson.InterledgerModule;
import org.interledger.quilt.jackson.conditions.Encoding;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.zalando.problem.ProblemModule;
import org.zalando.problem.violations.ConstraintViolationProblemModule;

/**
 * A factory for constructing instances of {@link ObjectMapper} for all connector components.
 */
public class ObjectMapperFactory {

  /**
   * Construct an {@link ObjectMapper} that can be used to serialize and deserialize JSON where all numbers are Strings,
   * by default.
   *
   * @return An {@link ObjectMapper}.
   */
  public static ObjectMapper create() {

    return new ObjectMapper()
      .registerModule(new Jdk8Module())
      .registerModule(new HttpUrlModule())
      .registerModule(new GuavaModule())
      .registerModule(new JavaTimeModule())
      .registerModule(new PaymentPointerModule())
      .registerModule(new InvoiceIdModule())
      .registerModule(new InterledgerModule(Encoding.BASE64))
      .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
      // Even though `false`` is the default setting for WRITE_NUMBERS_AS_STRINGS, we overtly set it here to alert
      // the reader that this value must be set this way in order to easily support Problems JSON, which per
      // https://tools.ietf.org/html/rfc7807#section-3.1 is specified to be a number (and not a String). This means
      // that we must be careful to decide if we want to serialize any other JSON number as a String (which generally
      // we do.
      .configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, true)
      .configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true)
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  /**
   * Construct an {@link ObjectMapper} that can be used to serialize and deserialize ProblemsJSON where JSON numbers
   * emit as non-String values. Because Problems+Json requires HTTP status codes to be serialized as numbers (and not
   * Strings) per RFC-7807, this ObjectMapper should not be used for payloads that involve Problems.
   *
   * @return An {@link ObjectMapper}.
   *
   * @see "https://tools.ietf.org/html/rfc7807"
   */
  public static ObjectMapper createObjectMapperForProblemsJson() {
    return create()
      .registerModule(new ProblemModule())
      .registerModule(new ConstraintViolationProblemModule())
      .configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, false);
  }
}
