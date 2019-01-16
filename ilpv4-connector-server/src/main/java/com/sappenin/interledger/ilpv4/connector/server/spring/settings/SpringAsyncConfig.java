package com.sappenin.interledger.ilpv4.connector.server.spring.settings;

import com.sappenin.interledger.ilpv4.connector.server.spring.CustomAsyncExceptionHandler;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Configures async and scheduling support in Spring.
 */
@Configuration
@EnableAsync
@EnableScheduling
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
