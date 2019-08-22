package org.interledger.connector.accounts;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import okhttp3.HttpUrl;
import org.immutables.value.Value;

import java.util.Map;
import java.util.Optional;

/**
 * <p>Defines settings related to Settlement.</p>
 *
 * <p>This object is optionally-present on an account, since some accounts do not settle.</p>
 */
@Value.Immutable(intern = true)
@JsonSerialize(as = ImmutableSettlementEngineDetails.class)
@JsonDeserialize(as = ImmutableSettlementEngineDetails.class)
public interface SettlementEngineDetails {

  static ImmutableSettlementEngineDetails.Builder builder() {
    return ImmutableSettlementEngineDetails.builder();
  }

  /**
   * The unique identifier of the Settlement Engine account, as created by calling `POST /accounts/:id` on the
   * Settlement Engine. Optional because this value will not be present until an account is created.
   */
  Optional<SettlementEngineAccountId> settlementEngineAccountId();

  /**
   * The base URL of the Settlement Engine.
   *
   * @return
   */
  HttpUrl baseUrl();

  /**
   * Additional, custom settings that any plugin can define.
   */
  Map<String, Object> customSettings();

}
