package com.sappenin.interledger.ilpv4.connector.server.spring.controllers.settlement;

import com.sappenin.interledger.ilpv4.connector.server.spring.settings.web.JacksonConfig;
import com.sappenin.interledger.ilpv4.connector.server.spring.settings.web.SecurityConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.zalando.problem.spring.web.advice.ProblemHandling;
import org.zalando.problem.spring.web.advice.security.SecurityAdviceTrait;

@Configuration
// Required to find the right handler.
@Import({SettlementController.class, SecurityConfiguration.class, JacksonConfig.class})
public class ControllerConfig { //implements WebMvcConfigurer {

  // Required for Problems support in test-harness
  @ControllerAdvice
  static class ExceptionHandling implements ProblemHandling, SecurityAdviceTrait {

  }

}
