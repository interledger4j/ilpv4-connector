package com.sappenin.interledger.ilpv4.connector.server.spring.settings.javamoney;

import org.springframework.beans.factory.serviceloader.ServiceListFactoryBean;
import org.springframework.core.Ordered;

import javax.money.spi.ServiceProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p>A custom JavaMoney {@link ServiceProvider} that integrates with Spring and this connector infrastructure.</</p>
 *
 * <p>This mechanism is registered with the standard Java ServiceLoader and overrides the default provider due to the
 * higher priority (method getPriority), allowing us to implement and register SPIs directly as Spring components.</p>
 *
 * <p>Note that this mechanism requires the usage of a `bootstrap` file to override the default JavaMoney {@link
 * ServiceProvider} with this one.</p>
 *
 * @see "https://jaxenter.de/java-in-the-enterprise-einfuehrung-in-das-money-and-currency-api-41763"
 * @see "https://github.com/JavaMoney/javamoney-lib/tree/master/javamoney-cdi"
 */
public class SpringServiceProvider implements ServiceProvider {

  /**
   * This method allows to define a priority for a registered ServiceProvider instance. It is set to the highest
   * possible value so that it is always used.
   *
   * @return the provider's priority
   */
  @Override
  public int getPriority() {
    return Ordered.HIGHEST_PRECEDENCE;
  }

  /**
   * Access a list of services, given its type. The bootstrap mechanism should order the instance for precedence, hereby
   * the most significant should be first in order. If no such services are found, an empty list should be returned.
   *
   * @param serviceType the service type.
   *
   * @return The instance to be used, never {@code null}
   */
  @Override
  public <T> List<T> getServices(Class<T> serviceType) {
    return ApplicationContextProvider.getAllApplicationContexts().values().stream().findFirst()
      .map(context -> context.getBeansOfType(serviceType))
      .map(Map::values)
      .map(ArrayList::new)
      .map(al -> (List<T>) al)
      .orElseGet(() -> {
        // Allows the normal ServiceLoader interface to operate.
        ServiceListFactoryBean serviceListFactoryBean = new ServiceListFactoryBean();
        serviceListFactoryBean.setServiceType(serviceType);
        try {
          return (List<T>) serviceListFactoryBean.getObject();
        } catch (Exception e) {

          throw new RuntimeException(e);
        }
      });
  }

}
