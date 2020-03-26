package org.interledger.connector.server.spring.controllers;

import com.fasterxml.jackson.databind.JsonMappingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.zalando.problem.StatusType;
import org.zalando.problem.ThrowableProblem;
import org.zalando.problem.spring.web.advice.ProblemHandling;
import org.zalando.problem.spring.web.advice.security.SecurityAdviceTrait;

import java.net.URI;

@ControllerAdvice
class ProblemExceptionHandling implements ProblemHandling, SecurityAdviceTrait {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  /**
   * Override for logging purposes.
   *
   * @see "https://github.com/zalando/problem-spring-web/issues/41"
   */
  @Override
  public ThrowableProblem toProblem(final Throwable throwable, final StatusType status, final URI type) {
    final ThrowableProblem problem = convert(throwable, status, type);

    if (!(throwable instanceof BadCredentialsException)) {
      logger.error(throwable.getMessage(), throwable);
    } else if (HttpMessageNotReadableException.class.isAssignableFrom(HttpMessageNotReadableException.class)) {
      // Here we should only emit the message as a WARN.
      logger.warn(throwable.getMessage());
    } else {
      logger.error(problem.getMessage(), throwable);
    }

    return problem;
  }

  /**
   * Helper method to convert a throwable into a {@link ThrowableProblem}.
   *
   * @param throwable The {@link Throwable} containing the actual error.
   * @param status    A {@link StatusType} for the current error.
   * @param type      A {@link URI} that uniquely identifies the error.
   *
   * @return A {@link ThrowableProblem}.
   */
  private ThrowableProblem convert(final Throwable throwable, final StatusType status, final URI type) {
    final ThrowableProblem throwableProblem;
    if (throwable != null) {
      if (HttpMessageNotReadableException.class.isAssignableFrom(throwable.getClass())) {
        final Throwable cause = throwable.getCause();
        if (cause != null && JsonMappingException.class.isAssignableFrom(cause.getClass())) {
          final Throwable secondCause = cause.getCause();
          if (secondCause != null && ThrowableProblem.class.isAssignableFrom(secondCause.getClass())) {
            throwableProblem = (ThrowableProblem) secondCause;
          } else {
            // default
            throwableProblem = prepare(throwable, status, type).build();
          }
        } else {
          // default
          throwableProblem = prepare(throwable, status, type).build();
        }
      } else {
        // default
        throwableProblem = prepare(throwable, status, type).build();
      }
    } else {
      // default
      throwableProblem = prepare(throwable, status, type).build();
    }

    final StackTraceElement[] stackTrace = createStackTrace(throwable);
    throwableProblem.setStackTrace(stackTrace);

    return throwableProblem;
  }


}
