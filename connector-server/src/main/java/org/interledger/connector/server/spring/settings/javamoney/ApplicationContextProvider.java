package org.interledger.connector.server.spring.settings.javamoney;

import com.google.common.collect.Maps;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Map;
import java.util.Optional;

/**
 * Exists to provide a static accessor for the ApplicationContext of the current Spring context so that JavaMoney can be
 * conencted into the Spring Bean framework.
 */
public class ApplicationContextProvider implements ApplicationContextAware {

  private static Map<String, ApplicationContext> ctx = Maps.newConcurrentMap();

  public static Map<String, ApplicationContext> getAllApplicationContexts() {
    return ctx;
  }

  public static ApplicationContext getApplicationContext(String contextId) {
    return ctx.get(contextId);
  }

  public void setApplicationContext(ApplicationContext ctx) throws BeansException {
    // Identify the context by the ILP Operator Address (falling back to the context-id), in order to support multiple
    // topologies in the same JVM.
    final String contextId = Optional
      .ofNullable(ctx.getEnvironment().getProperty("interledger.connector.nodeIlpAddress"))
      .orElseGet(() -> ctx.getId());

    this.ctx.put(contextId, ctx);
  }
}
