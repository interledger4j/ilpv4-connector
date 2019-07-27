package com.sappenin.interledger.ilpv4.connector.server.spring.controllers;

import com.sappenin.interledger.ilpv4.connector.server.spring.controllers.settlement.SettlementController;
import com.sappenin.interledger.ilpv4.connector.server.spring.settings.web.IdempotenceCacheConfig;
import com.sappenin.interledger.ilpv4.connector.server.spring.settings.web.JacksonConfig;
import com.sappenin.interledger.ilpv4.connector.server.spring.settings.web.SecurityConfiguration;
import org.interledger.ilpv4.connector.config.RedisConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.zalando.problem.spring.web.advice.ProblemHandling;
import org.zalando.problem.spring.web.advice.security.SecurityAdviceTrait;

@Configuration
@Import({
          SettlementController.class,
          IdempotenceCacheConfig.class,
          SecurityConfiguration.class,
          RedisConfig.class, // Required for the JedisConnectionFactory bean, even if Redis is disabled.
          JacksonConfig.class
        })
public class ControllerConfig {

  // Required for Problems support in test-harness
  @ControllerAdvice
  static class ExceptionHandling implements ProblemHandling, SecurityAdviceTrait {

  }

}
