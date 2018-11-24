package com.sappenin.interledger.ilpv4.connector.server.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

import java.lang.reflect.Method;

/**
 * When a method return type is a Future, exception handling is easy – Future.getEntry() method will throw the exception.
 *
 * But, if the return type is void, exceptions will not be propagated to the calling thread. Hence we need to add extra
 * configurations to handle exceptions.
 */
public class CustomAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

  Logger logger = LoggerFactory.getLogger(this.getClass());

  @Override
  public void handleUncaughtException(Throwable throwable, Method method, Object... obj) {
    final String exceptionMessage = throwable.getMessage();
    final String methodName = method.getName();

    final StringBuilder methodParams = new StringBuilder();
    for (Object param : obj) {
      methodParams.append(param);
    }

    logger
      .error("methodName: {} params: {} exceptionMessage: {} ", methodName, methodParams.toString(), exceptionMessage);
  }

}