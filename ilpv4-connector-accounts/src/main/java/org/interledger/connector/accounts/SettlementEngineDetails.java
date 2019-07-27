package org.interledger.connector.accounts;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import okhttp3.HttpUrl;
import org.immutables.value.Value;
import org.interledger.core.InterledgerAddress;

import java.util.Optional;

/**
 * <p>Defines settings related to Settlement.</p>
 *
 * <p>This object is optionally-present on an account, since some accounts do not settle.</p>
 */
public interface SettlementEngineDetails {

  static ImmutableSettlementEngineDetails.Builder builder() {
    return ImmutableSettlementEngineDetails.builder();
  }

  /**
   * The unique identifier of the Settlement Engine account, as created by calling `POST /accounts/:id` on the
   * Settlement Engine.
   *
   * @return
   */
  String settlementEngineAccountId();

  /**
   * <p>The asset scale the settlement engine is configured to use. This value is the order of magnitude that the
   * Settlement Engine views 1 single unit of an underlying asset type. For example, if a Settlement Engine is operating
   * in XRP, then the core unit of value is 1 XRP. Specifying a scale of 6 would mean that the engine will treat 1 unit
   * as a "drop" (1 millionth of an XRP). Conversely, if the Connector instructs the Settlement Engine to settle 1 unit,
   * then the Settlement Engine would settle only a single drop.</p>
   *
   * <p>The Connector's internal machinery can utilize the assetScale of an Account, and the assetScale of a
   * corresponding Settlement Engine in order to be able to translate ILP clearing payment values into the correct scale
   * for Settlement Engine transactions, and vice-versa.</p>
   *
   * // TODO: Once the RFC is finalized, update this Javadoc.
   *
   * @return An integer representing the SE's asset scale.
   */
  int assetScale();

  /**
   * The base URL of the Settlement Engine.
   *
   * @return
   */
  HttpUrl baseUrl();

  @Value.Immutable(intern = true)
  @JsonSerialize(as = ImmutableSettlementEngineDetails.class)
  @JsonDeserialize(as = ImmutableSettlementEngineDetails.class)
  abstract class AbstractSettlementEngineDetails implements SettlementEngineDetails {

  }
}
