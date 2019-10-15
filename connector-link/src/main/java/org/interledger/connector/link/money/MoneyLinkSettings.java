package org.interledger.connector.link.money;

import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Redacted;
import org.interledger.connector.link.Link;
import org.interledger.connector.link.LinkSettings;

import java.util.Map;

/**
 * Configuration information relating to a {@link MoneyLink}.
 *
 * @deprecated Consider an interface outside of {@link Link} for settlement.
 */
@Deprecated
public interface MoneyLinkSettings extends LinkSettings {

  static ImmutableMoneyLinkSettings.Builder builder() {
    return ImmutableMoneyLinkSettings.builder();
  }

  @Immutable
  abstract class AbstractMoneyLinkSettings implements MoneyLinkSettings {

    /**
     * Additional, custom settings that any link can define. Redacted to prevent credential leakage in log files.
     */
    @Redacted
    public abstract Map<String, Object> customSettings();

  }

}
