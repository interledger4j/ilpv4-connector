package org.interledger.connector.server.spring.settings;

import org.interledger.connector.server.spring.CustomAsyncExceptionHandler;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.scheduling.annotation.AsyncConfigurer;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Configures async and scheduling support in Spring.
 */
//@Configuration
//@EnableAsync
//@EnableScheduling
public class SpringAsyncConfig implements AsyncConfigurer {

  @Override
  public Executor getAsyncExecutor() {
    // TODO: Make this configurable
    return new ScheduledThreadPoolExecutor(4);
  }

  @Override
  public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
    return new CustomAsyncExceptionHandler();
  }

}
