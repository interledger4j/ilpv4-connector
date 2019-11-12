package org.interledger.connector.server.spring.settings.javamoney;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Exists to provide a static accessor for the ApplicationContext of the current Spring context so that JavaMoney can be
 * connected into the Spring Bean framework.
 */
public class ApplicationContextProvider implements ApplicationContextAware {

  private static Set<ApplicationContext> ctx = Sets.newConcurrentHashSet();

  /**
   * Returns all active application contexts.
   * @return set of active contexts
   */
  public static Set<ApplicationContext> getAllApplicationContexts() {
    return findActiveContexts(ctx);
  }

  @VisibleForTesting
  static Set<ApplicationContext> findActiveContexts(Set<ApplicationContext> contexts) {
    return contexts.stream()
        .filter(ctx -> ctx instanceof ConfigurableApplicationContext)
        .map(ctx -> (ConfigurableApplicationContext) ctx)
        .filter(ctx -> ctx.isActive())
        .collect(Collectors.toSet());
  }

  public void setApplicationContext(ApplicationContext ctx) throws BeansException {
    this.ctx.add(ctx);
  }
}
