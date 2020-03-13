package org.interledger.connector.config;

import static org.interledger.connector.core.ConfigConstants.DOT;

import org.interledger.connector.core.ConfigConstants;

import com.google.common.base.Preconditions;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;

import java.util.Objects;
import java.util.Optional;

/**
 * A Spring conditional that decides whether or not to enable the Connector's STREAM receiver
 */
public class SpspReceiverEnabledCondition implements Condition {

  private static final String LOCAL_SPSP_FULFILLMENT_ENABLED =
    ConfigConstants.ENABLED_FEATURES + DOT + ConfigConstants.LOCAL_SPSP_FULFILLMENT_ENABLED;
  private static final String SPSP_ENABLED = ConfigConstants.ENABLED_PROTOCOLS + DOT + ConfigConstants.SPSP_ENABLED;

  /**
   * Determine if a STREAM Receiver should be enabled in the Connector. STREAM receiver should be enabled if either 1)
   * The Connector is operating packet-switch mode AND local SPSP termination is ENABLED; or 2) The Connector is
   * operating in wallet mode AND SPSP is not DISABLED.
   *
   * @param context  the condition context
   * @param metadata metadata of the {@link AnnotationMetadata class} or {@link MethodMetadata method} being checked
   *
   * @return {@code true} if the condition matches and the component can be registered, or {@code false} to veto the
   *   annotated component's registration
   */
  @Override
  public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
    final Environment env = context.getEnvironment();

    if (isSpspEnabled(env)) {
      Preconditions.checkArgument(isLocalSpspFulfillmentEnabled(env),
        "Local SPSP fulfillment may not be disabled if `%s` is enabled", SPSP_ENABLED
      );
    }

    if (isLocalSpspFulfillmentEnabled(env)) {
      return true;
    } else {
      return isSpspEnabled(env);
    }
  }

  private boolean isLocalSpspFulfillmentEnabled(final Environment env) {
    Objects.requireNonNull(env);
    return Optional.ofNullable(env.getProperty(LOCAL_SPSP_FULFILLMENT_ENABLED))
      .filter(val -> val.equalsIgnoreCase(ConfigConstants.TRUE))
      .map(val -> true)
      .orElse(false);
  }

  private boolean isSpspEnabled(final Environment env) {
    Objects.requireNonNull(env);
    return Optional.ofNullable(env.getProperty(SPSP_ENABLED))
      .filter(val -> val.equalsIgnoreCase(ConfigConstants.TRUE))
      .map(val -> true)
      .orElse(false);
  }
}
