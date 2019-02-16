package org.interledger.connector.link;

import org.immutables.value.Value;
import org.interledger.core.InterledgerAddress;

import java.util.Map;

/**
 * Configuration information relating to a {@link Link}.
 */
public interface LinkSettings {

  static ImmutableLinkSettings.Builder builder() {
    return ImmutableLinkSettings.builder();
  }

  /**
   * The type of this ledger link.
   */
  LinkType getLinkType();

  /**
   * The ILP address of the ILP Node operating this link.
   */
  InterledgerAddress getOperatorAddress();

  /**
   * Additional, custom settings that any link can define.
   */
  Map<String, Object> getCustomSettings();

  @Value.Immutable
  abstract class AbstractLinkSettings implements LinkSettings {

    /**
     * Additional, custom settings that any link can define. Redacted to prevent credential leakage in log files.
     */
    @Value.Redacted
    public abstract Map<String, Object> getCustomSettings();

  }
}
