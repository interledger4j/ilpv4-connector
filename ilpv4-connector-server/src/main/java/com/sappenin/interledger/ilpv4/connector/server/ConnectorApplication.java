package com.sappenin.interledger.ilpv4.connector.server;

// A convenience annotation that adds all of the following:
// @Configuration, @EnableAutoConfiguration, @EnableWebMvc,and @ComponentScan
//@SpringBootApplication
public class ConnectorApplication {

  public static void main(String[] args) {

    //SpringApplication.run(ConnectorApplication.class, args);
    new ConnectorServer().start();
  }

}

