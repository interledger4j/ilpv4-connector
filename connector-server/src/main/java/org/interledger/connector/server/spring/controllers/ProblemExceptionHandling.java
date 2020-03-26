package org.interledger.connector.server.spring.controllers;

import org.interledger.connector.accounts.AccountProblem;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.collect.Sets;
import org.apache.http.auth.InvalidCredentialsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.zalando.problem.StatusType;
import org.zalando.problem.ThrowableProblem;
import org.zalando.problem.spring.web.advice.ProblemHandling;
import org.zalando.problem.spring.web.advice.security.SecurityAdviceTrait;

import java.net.URI;
import java.util.Set;

@ControllerAdvice
class ProblemExceptionHandling implements ProblemHandling, SecurityAdviceTrait {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final Set<Class> warnExceptions;
  private final Set<Class> warnWithoutStackTraces;
  private final Set<Class> ignoredExceptions;

  /**
   * No-args Exception.
   */
  private ProblemExceptionHandling() {
    this.warnExceptions = Sets.newHashSet();
    this.warnWithoutStackTraces = Sets.newHashSet(
      // Generally just warnings about invalid account info, so stack-traces aren't helpful.
      AccountProblem.class
    );
    this.ignoredExceptions = Sets.newHashSet(
      // Logged by Spring AdviceTraits, so no need to log again
      InsufficientAuthenticationException.class,
      // Logged by Spring AdviceTraits, so no need to log again
      BadCredentialsException.class
    );
  }

  /**
   * Override for logging purposes.
   *
   * @see "https://github.com/zalando/problem-spring-web/issues/41"
   */
  @Override
  public ThrowableProblem toProblem(final Throwable throwable, final StatusType status, final URI type) {
    if (warnWithoutStackTraces.contains(throwable.getClass())) {
      // Here we should only emit the message, and only as a WARN.
      logger.warn(throwable.getMessage());
    } else if (warnExceptions.contains(throwable.getClass())) {
      logger.warn(throwable.getMessage(), throwable);
    } else if (ignoredExceptions.contains(throwable.getClass())) {
      // Ignore the exception.
    } else {
      // Log anything else as an error.
      logger.error(throwable.getMessage(), throwable);
    }

    // No need to log the converted problem because either 1) It was logged above as a Throwable and 2) the Problem
    // details are user-facing, whereas the Throwable is helpful for finding errors in Java code.
    return this.toConnectorProblem(throwable, status, type);
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
  private ThrowableProblem toConnectorProblem(final Throwable throwable, final StatusType status, final URI type) {
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
