package com.sappenin.ilpv4.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

// A convenience annotation that adds all of the following:
// @Configuration, @EnableAutoConfiguration, @EnableWebMvc,and @ComponentScan
@SpringBootApplication
@Import(ConnectorConfiguration.class)
public class ConnectorApplication {

  public static void main(String[] args) {
    SpringApplication.run(ConnectorApplication.class, args);
  }

}

