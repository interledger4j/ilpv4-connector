package com.sappenin.interledger.ilpv4.connector.server;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Properties;

/**
 * A Server that runs on a particular port, with various properties. This class is used to be able to operate multiple
 * instances on different ports in the same JVM, which is especially useful for integration testing purposes.
 */
public class Server {

  public static final String ILP_SERVER_PORT = "interledger.server.port";

  private final Properties properties;
  protected SpringApplication application;
  private ConfigurableApplicationContext context;

  public Server(Class<?>... configurations) {
    this.application = new SpringApplication(configurations);
    this.properties = new Properties();

    final ArrayList<ApplicationContextInitializer<?>> initializers = new ArrayList<>();
    initializers.add(context -> context.getEnvironment().getPropertySources()
      .addFirst(new PropertiesPropertySource("node", properties)));
    application.setInitializers(initializers);
  }

  public void start() {
    context = application.run();
  }

  public void stop() {
    if (context != null) {
      context.close();
    }
  }

  public Server setProperty(String key, String value) {
    this.properties.setProperty(key, value);
    return this;
  }

  public ConfigurableApplicationContext getContext() {
    return context;
  }

  public int getPort() {
    // Take the port out of the properties...
    return Optional.ofNullable(this.properties.getProperty(ILP_SERVER_PORT))
      .map(Integer::new)
      .orElseGet(() -> {
        if (context != null) {
          return context.getBean(ConnectorServerConfig.class).getPort();
        } else {
          throw new IllegalStateException("Server not started!");
        }
      });
  }
}
