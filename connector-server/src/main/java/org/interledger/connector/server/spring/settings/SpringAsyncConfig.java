package org.interledger.connector.server.spring.settings;

import org.interledger.connector.payments.DefaultIdlePendingPaymentsCloser;
import org.interledger.connector.payments.IdlePendingPaymentsCloser;
import org.interledger.connector.payments.StreamPaymentManager;
import org.interledger.connector.server.spring.CustomAsyncExceptionHandler;

import com.google.common.eventbus.EventBus;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
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


  @Bean
  protected IdlePendingPaymentsCloser expiredPaymentCloser(EventBus eventBus, StreamPaymentManager paymentManager) {
    return new DefaultIdlePendingPaymentsCloser(eventBus, paymentManager);
  }

}
